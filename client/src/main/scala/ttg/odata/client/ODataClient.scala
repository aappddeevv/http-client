// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg.odata
package client

import scala.scalajs.js
import scala.concurrent.{Future, ExecutionContext}
import js.annotation._
import fs2._
import cats._
import cats.data._
import cats.effect._
import fs2._
import js.JSConverters._

import ttg.odata._
import http._
import client.syntax.queryspec._

/** An OData client that can raise an error. */
trait ClientError[F[_]] {
  /** Raise an error that is captured in the `F` context. */
  def raiseError[A](resp: HttpResponse[F], msg: String, req: Option[HttpRequest[F]]): F[A]
}

/**
  * OData specific client. Its a thin layer over a basic HTTP client that
  * formulates the HTTP request and minimually interprets the response. This
  * class formulate the HttpRequest in an OData specific way then creates the
  * suspend acion to execute the request and process the result. You'll need to
  * construct this carefully for each specific OData backend. Since OData
  * datasources can have a number of application specific options, this trait
  * should be considered a building black that has many type parameters that
  * reflect the optionality.
  *
  * All of the methods either return an F or a Steam. The F or Stream must be
  * run in order to execute the operation. The client only captures the most
  * commonly used idioms of an OData web service. It's possible to have cases
  * here this client's API is insufficient for your OData url.
  *
 * Subclasses must define error handling. While the outer structure of an error
 * is defined in he OData spec, the details are application specific.
  *
 * @tparam F Effect for requests, must also be able to hold an error.
 * @tparam RO RequestOptions type. Allows you to customize OData per-request
 * parameters.
  */
trait ODataClient[F[_], PO <: BasicPreferOptions, RO <: RequestOptions[PO]]
    extends ODataRequests[F, PO, RO]
    with ClientOps[F]
    with ClientError[F]{ self =>

  def http: Client[F]
  implicit def F: MonadError[F, Throwable]

  /** We need the base URL to potentiailly generate some of the OData requests, specifically
   * the multipart batch requests. You can also use to set the HOST HTTP header.
    */
  val base: String

  /**
    * Update a single property, return the id updated.
@see https://docs.microsoft.com/en-us/dynamics365/customer-engagement/developer/webapi/update-delete-entities-using-web-api?view=dynamics-ce-odata-9
    */
  def updateOneProperty(entitySet: String,
                        id: String,
                        property: String,
                        value: js.Any,
                        opts: Option[RO]=None): F[String] = {
    val b = js.JSON.stringify(js.Dynamic.literal("value" -> value))
    // A PUT! not POST!
    val request =
      HttpRequest[F](Method.PUT, s"/$entitySet($id)/$property", body = Entity.fromString(b), headers = toHeaders(opts))
    http.fetch[String](request) {
      case Status.Successful(resp) if (resp.status == Status.NoContent) => F.pure(id)
      case failedResponse                                               =>
        raiseError(failedResponse, s"Update $entitySet($id)", Option(request))
    }
  }

  /**
    * Run a batch request. Dynamics batch requests are POSTs.
    * @param headers Headers for the post. *not* for each request.
    * @param opts dynamics options such as "prefer"
    */
  def batch[R](m: Multipart[F], headers: HttpHeaders = HttpHeaders.empty, opts: Option[RO] = None)(
      implicit enc: EntityEncoder[F, Multipart[F]], dec: EntityDecoder[F, R]): F[R] = {
    val (xtras, ent) = enc.toEntity(m)
    val therequest =
      HttpRequest[F](Method.POST, "/$batch", headers = headers ++ toHeaders(opts) ++ xtras, body = ent)
    http.fetch[R](therequest) {
      case Status.Successful(resp) => resp.as[R]
      case failedResponse =>
        raiseError(failedResponse, s"Batch", Option(therequest))
    }
  }

  /**
    * Update an entity. Fails if no odata-entityid is returned in the header.
    * For now, do not use return=representation.
    *
    */
  def update[B](entitySet: String,
                id: String,
                body: B,
                upsertPreventCreate: Boolean = true,
                upsertPreventUpdate: Boolean = false,
                opts: Option[RO] = None)(implicit e: EntityEncoder[F,B]): F[String] = {
    val request =
      mkUpdateRequest[B](entitySet, id, body, upsertPreventCreate, upsertPreventUpdate, opts, Some(base))
    //HttpRequest(Method.PATCH, s"/$entitySet($id)", body = Entity.fromString(body), headers = toHeaders(opts) ++ h )
    http.fetch[String](request) {
      case Status.Successful(resp) => F.pure(id)
      case failedResponse =>
        raiseError(failedResponse, s"Update $entitySet($id)", Option(request))
    }
  }

  /** Create an entity and expect only its id returned. includeRepresentation is set to an explicit
    * false in the headers to ensure the id is returend in the header.
    */
  def createReturnId[B](entityCollection: String, body: B, opts: Option[RO] = None)(
      implicit e: EntityEncoder[F, B]): F[String] = {
    val newOpts = opts.copy(prefers = opts.prefers.copy(includeRepresentation = Some(false)))
    create(entityCollection, body, newOpts)(e, ReturnedIdDecoder)
  }

  /** Create an entity. If return=representation then the decoder can decode the body with entity content.
    * You can return an id or body from this function.
    */
  def create[B, R](entitySet: String, body: B, opts: Option[RO] = None)(
      implicit e: EntityEncoder[F, B],
      d: EntityDecoder[F, R]): F[R] = {
    //val request = HttpRequest(Method.POST, s"/$entitySet", body=Entity.fromString(body), headers=toHeaders(opts))
    val request = mkCreateRequest[B](entitySet, body, opts)
    http.fetch(request) {
      case Status.Successful(resp) => resp.as[R]
      case failedResponse =>
        raiseError(failedResponse, s"Create for $entitySet", Option(request))
    }
  }

  /** Delete an entity. Return the id passed in for convenience. Return true if
    * the entity does not exist even though this call did not technically delete
    * it if it did not exist already.
    * @param entityCollection Entity
    * @param keyInfo Primary key (GUID) or alternate key.
    * @return Pair of the id passed in and true if deleted (204), false if not (404).
    */
  def delete(entitySet: String,
             keyInfo: ODataId,
             opts: Option[RO] = None): F[(ODataId, Boolean)] = {
    // Status 204 indicates success, status 404 indicates the entity did not exist.
    val request = mkDeleteRequest(entitySet, keyInfo, opts)
    http.fetch(request) {
      case Status.Successful(resp)                               => F.pure((keyInfo, true))
      case Status.ClientError(resp) if (resp.status.code == 404) => F.pure((keyInfo, true))
      case failedResponse =>
        responseToFailedTask(failedResponse, s"Delete for $entitySet($keyInfo)", Option(request))
    }
  }

  /**
    * Execute an action. Can be bound or unbound depending on entityCollectionAndId. Action
    * name may be prefixed with a namespace like `Microsoft.Dynamics.CRM`.
    *
    * TODO: Map into a http.expect.
    */
  def executeAction[A, B](action: String,
                       body: A,
                       entitySetAndId: Option[(String, String)] = None,
                       opts: Option[RO]=None)(implicit E: EntityEncoder[F, A], D: EntityDecoder[F, B]): F[B] = {
    val request = mkExecuteActionRequest(action, body, entitySetAndId, opts)
    http.fetch(request) {
      case Status.Successful(resp) => resp.as[B]
      case failedResponse =>
        raiseError(failedResponse, s"Executing action $action, $body, $entitySetAndId", Option(request))
    }
  }

  /** Exceute a bound or unbound function.
    *
    * @param function Function name
    * @param parameters Query parameters for function.
    * @param entity Optional (entityCollection, id) info for bound function call. Use None for unbound function call.
    * @param d Decode response body.
    * @return
    */
  def executeFunction[A](function: String,
                         parameters: Map[String, scala.Any] = Map.empty,
                         entity: Option[(String, String)] = None)(implicit d: EntityDecoder[F, A]): F[A] = {
    val req = mkExecuteFunctionRequest(function, parameters, entity)
    http.expect(req)(d)
  }

  /**
    * Associate an existing entity to another through a single or collection
    * valued navigation property.
    */
  def associate(fromEntitySet: String,
                fromEntityId: String,
                navProperty: String,
                toEntitySet: String,
                toEntityId: String,
                singleNavProperty: Boolean): F[Boolean] = {
    val request =
      mkAssociateRequest(fromEntitySet, fromEntityId, navProperty, toEntitySet, toEntityId, base, singleNavProperty)
    http.fetch(request) {
      case Status.Successful(resp) => F.pure(true)
      case failedResponse =>
        raiseError(failedResponse,
                             s"Association $fromEntitySet($fromEntityId)->$navProperty->$toEntitySet($toEntityId)",
                             Option(request))
    }
  }

  /** Disassociate an entity fram a navigation property. */
  def disassociate(fromEntitySet: String,
                   fromEntityId: String,
                   navProperty: String,
                   to: Option[String] = None): F[Boolean] = {
    val request = mkDisassocatiateRequest(fromEntitySet, fromEntityId, navProperty, to)
    http.fetch(request) {
      case Status.Successful(resp) => F.pure(true)
      case failedResponse =>
        raiseError(failedResponse,
                             s"Disassociation $fromEntitySet($fromEntityId)->$navProperty->$to",
                             Option(request))
    }
  }

  /**
    * Get a single entity using key information. The keyInfo can be a guid or
    * alternate key criteria e.g "altkeyattribute='Larry',...". You get all the
    * fields with this. This assumes that the entity exists since you providing
    * it in id--you should know ahead of time.
    *
    * Allow you to specify a queryspec somehow as well.
    *
    * @todo Return an Option?
    */
  def getOneWithKey[A](entitySet: String,
                       keyInfo: ODataId,
                       attributes: Seq[String] = Nil,
                       opts: Option[RO]=None)(implicit d: EntityDecoder[F, A]): F[A] = {
    val q = QuerySpec(select = attributes)
    getOne(q.url(s"/$entitySet(${keyInfo.render()})"), opts)(d)
  }

  /**
    * Get one entity using a full query url. You can use a QuerySpec to form the
    * url then call `qs.url(myentities,Some(theid))`.
    *
    * If you use a URL that returns a OData response with a `value` array that
    * needs be automatically extracted, you need to use an explict EntityDecoder
    * that first looks for that array then obtains your `A`. See
    * `EntityDecoder.ValueWrapper` for an example.
    *
    * You also use this pattern
    * when employing `getOne` to obtain related records in a 1:M navigation
    * property e.g. a single entity's set of connections or some child
    * entity. In this case, your URL will typically have an "expand" segment.
    * Note that if you navigate to a simple attribute, then it is returned as a
    * simple object also attached to "value" so choose your decoder wisely.
    *
    * @todo Return an Option?
    */
  def getOne[A](url: String, opts: Option[RO]=None)(
      implicit d: EntityDecoder[F, A]): F[A] = {
    val request = HttpRequest[F](Method.GET, url, headers = toHeaders(opts), Entity.Empty[F])
    http.fetch(request) {
      case Status.Successful(resp) => resp.as[A]
      case failedResponse =>
        raiseError(failedResponse, s"Get one entity $url", Option(request))
    }
  }

  /**
    * Get a list of values. Follows @data.nextLink but accumulates all the
    * results into memory. Prefer [[getListStream]]. For now, the caller must
    * decode external to this method. The url is typically created from a
    * QuerySpec.
    *
    * @see getListStream
    */
  def getList[A <: js.Any](url: String, opts:Option[RO]=None)(): F[Seq[A]] =
    _getListStream[A](url, toHeaders(opts)).compile.toVector

  /**
    * Get a list of values as a stream. Follows @odata.nextLink. For now, the
    * caller must decode external to this method. The url is usually created
    * from a QuerySpec e.g. `val q = QuerySpec(); val url = q.url("entitysetname")`.
    */
  def getListStream[A <: js.Any](url: String, opts:Option[RO]=None): Stream[F, A] =
    _getListStream[A](url, toHeaders(opts))
}
