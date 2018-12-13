// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg.odata
package client

import scala.scalajs.js
import js.annotation._
import cats.Monad
import http._

/** 
 * Request creation may involve "client" specific extensions to how OData
 * headers are created so we expose that explicitly and make it easier to
 * compose with other traits by switching from type parameters to abstract
 * types. Create OData HTTP requests given various input parameters.  We are
 * uneven in the use of key and where they are rendered in this algebra or
 * before this algebra is used. A renderer to render the RequestOptions type is
 * required and hence recursively, a renderer for Prefer values.
 */
trait ClientRequests[F[_]] {
  self: ClientIdRenderer with ClientFConstraints[F] =>
  type PreferOptions <: BasicPreferOptions
  type RequestOptions <: BasicRequestOptions[PreferOptions]

  private implicit val _F: Monad[F] = F

  /** Renderer for `RequestOptions`. */
  val optRenderer: HeaderRenderer[RequestOptions]
  // needs to be lazy, bug in compiler?
  def emptyBody: Entity[F] = Entity(F.pure(""))//Entity.empty[F]

  //val DefaultBatchRequest = HttpRequest(Method.PUT, "/$batch", body=emptyBody)

  /**
    * This does not handle the version tag + applyOptimisticConcurrency flag yet.
    */
  def toHeaders(o: Option[RequestOptions]): HttpHeaders = {
    o.fold(HttpHeaders.empty)(optRenderer(_))
    //prefer.map(str => HttpHeaders("Prefer"        -> str)).getOrElse(HttpHeaders.empty)
    //++ o.version.map(etag => HttpHeaders("If-None-Match" -> etag)).getOrElse(HttpHeaders.empty)
  }

  def mkGetListRequest(url: String, opts: Option[RequestOptions] = None) =
    HttpRequest[F](Method.GET, url, headers = toHeaders(opts), emptyBody)

  def mkCreateRequest[B](entitySet: String, body: B, opts: Option[RequestOptions] = None)(
      implicit e: EntityEncoder[F,B]) = {
    // HttpRequest(Method.POST, s"/$entitySet", body = Entity.fromString(body), headers = toHeaders(opts))
    val (xtras, ent) = e.toEntity(body)
    HttpRequest[F](Method.POST, s"/$entitySet", body = ent, headers = toHeaders(opts) ++ xtras)
  }

  /** Make a pure delete request. */
  def mkDeleteRequest(entitySet: EntitySetName, key: ODataId, opts: Option[RequestOptions] = None) = {
    val etag = opts.fold(HttpHeaders.empty){o =>
      if (o.applyOptimisticConcurrency.getOrElse(false) && o.version.isDefined)
        HttpHeaders("If-Match" -> o.version.get)
      else HttpHeaders.empty
    }
    HttpRequest[F](Method.DELETE, s"/${entitySet.asString}(${renderId(key)})", headers = toHeaders(opts) ++ etag,
      body = emptyBody)
  }

  def mkGetOneRequest(url: String, opts: Option[RequestOptions] = None) = {
    val etag = opts.flatMap(_.version.map(etag => HttpHeaders("If-None-Match" -> etag))).getOrElse(HttpHeaders.empty)
    HttpRequest[F](Method.GET, url, headers = toHeaders(opts) ++ etag, emptyBody)
  }

  def mkExecuteActionRequest[A](action: String,
    body: A,
    entitySetAndId: Option[(String, String)] = None,
    opts: Option[RequestOptions] = None)(
    implicit E: EntityEncoder[F, A]) = {
    val url = entitySetAndId.map { case (c, i) => s"/$c($i)/$action" }.getOrElse(s"/$action")
    val (hdrs, ent) = E.toEntity(body)
    HttpRequest[F](Method.POST, url, body = ent, headers = toHeaders(opts) ++ hdrs)
  }

  /**
    * Not sure adding $base to the @odata.id is absolutely needed. Probably is.
    * @see https://docs.microsoft.com/en-us/dynamics365/customer-engagement/developer/webapi/associate-disassociate-entities-using-web-api?view=dynamics-ce-odata-9
    */
  def mkAssociateRequest(
    fromEntitySet: EntitySetName,
    fromEntityId: ODataId,
    navProperty: String,
    toEntitySet: EntitySetName,
    toEntityId: ODataId,
    base: String,
    singleValuedNavProperty: Boolean = true)(
    implicit enc: EntityEncoder[F,String]
  ): HttpRequest[F] = {
    val url  = s"/${fromEntitySet.asString}(${renderId(fromEntityId)})/$navProperty/$$ref"
    val body = s"""{"@odata.id": "$base/${toEntitySet.asString}(${renderId(toEntityId)})"}"""
    val method =
      if (singleValuedNavProperty) Method.PUT
      else Method.POST
    val (xtras, ent) = enc.toEntity(body)
    HttpRequest(method, url, body = ent)
  }

  /**
    * Provide `to` if its a collection-valued navigation property, otherwise it
    * removes a single-valued navigation property.
    *
    * @see https://docs.microsoft.com/en-us/dynamics365/customer-engagement/developer/webapi/associate-disassociate-entities-using-web-api?view=dynamics-ce-odata-9
    */
  def mkDisassocatiateRequest(fromEntitySet: EntitySetName,
    fromEntityId: ODataId,
    navProperty: String,
    toId: Option[String]): HttpRequest[F] = {
    val navPropertyStr = toId.map(id => s"$navProperty($id)").getOrElse(navProperty)
    val url            = s"/${fromEntitySet.asString}(${renderId(fromEntityId)})/$navPropertyStr/$$ref"
    HttpRequest(Method.DELETE, url, body = emptyBody)
  }

  /**
    * Create a PATCH request that could also upsert. "opts" version could
    * override upsertPreventCreate if a version value is also included, so be careful.
    */
  def mkUpdateRequest[B](entitySet: String,
    id: String,
    body: B,
    upsertPreventCreate: Boolean = true,
    upsertPreventUpdate: Boolean = false,
    options: Option[RequestOptions] = None,
    base: Option[String] = None)(implicit enc: EntityEncoder[F,B]): HttpRequest[F] = {
    val (xtra, ent) = enc.toEntity(body)
    val h1 =
      if (upsertPreventCreate) HttpHeaders("If-Match" -> "*")
      else HttpHeaders.empty
    val h2 =
      if (upsertPreventUpdate) HttpHeaders("If-None-Match" -> "*")
      else HttpHeaders.empty
    // this may override If-Match! */
    val h3 = 
      options.fold(HttpHeaders.empty)(opts =>
        if (opts.applyOptimisticConcurrency.getOrElse(false) && opts.version.isDefined)
          HttpHeaders("If-Match" -> opts.version.get)
        else
          HttpHeaders.empty
      )
    val mustHave = HttpHeaders.empty ++ Map("Content-Type" -> Seq("application/json", "type=entry"))
    HttpRequest(Method.PATCH,
      s"${base.getOrElse("")}/$entitySet($id)",
      toHeaders(options) ++ h1 ++ h2 ++ h3 ++ xtra ++ mustHave,
      ent)
  }

  /**
   * Parameters are set into the URL as is without quotes even if its a string.
   * Explicitly provide quotes in your paramater values if you want them. 
   */
  def mkExecuteFunctionRequest(function: String,
    parameters: Map[String, scala.Any] = Map.empty,
    entity: Option[(String, String)] = None) = {
    // val q: Seq[(String, String)] = parameters.keys.zipWithIndex
    //   .map(x => (x._1, x._2 + 1))
    //   .map {
    //     case (k, i) =>
    //       parameters(k) match {
    //         //case s: String => (s"$k=@p$i", s"@p$i='$s'")
    //         case x @ _     => (s"$k=@p$i", s"@p$i=$x")
    //       }
    //   }
    //   .toSeq
    val q = parameters.map{ case(k,v) =>  s"$k=$v"}


    //val pvars        = q.map(_._1).mkString(",")
    //val pvals        = (if (q.size > 0) "?" else "") + q.map(_._2).mkString("&")
    //val functionPart = s"/$function($pvars)$pvals"
    val functionPart = s"""/$function(${q.mkString(",")})"""

    val entityPart = entity.map(p => s"/${p._1}(${p._2})").getOrElse("")

    val url = s"$entityPart$functionPart"
    HttpRequest[F](Method.GET, url, body=emptyBody)
  }

  /** @depecated. Use `mkBatch`. */
  def mkBatchRequest[A](headers: HttpHeaders, m: Multipart[F])(
  implicit enc: EntityEncoder[F,Multipart[F]]): HttpRequest[F] = mkBatch(m, headers)

  /**
    * Body in HttpRequest is ignored and is instead generated from m.
    * Since the parts will have requests, you need to ensure that the
    * base URL used in those requests have a consistent base URL.
    */
  def mkBatch(m: Multipart[F], headers: HttpHeaders = HttpHeaders.empty)(
    implicit enc: EntityEncoder[F,Multipart[F]]): HttpRequest[F] = {
    val (xtras, ent) = enc.toEntity(m)
    HttpRequest(Method.POST, "/$batch", headers = headers ++ xtras, body = ent)
  }
}
