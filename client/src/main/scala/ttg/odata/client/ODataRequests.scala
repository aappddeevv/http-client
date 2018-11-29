// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg.odata
package client

import scala.scalajs.js
import js.annotation._

import http._

import http.instances.entityencoder._
import http.instances.entitydecoder._

/** Create OData HTTP requests given various input parameters. */
trait ODataRequests[F[_], PO <: BasicPreferOptions, RO <: RequestOptions[PO]] {

  /** Renderer for `RO`. */
  val roRenderer: HeaderRenderer[RO]
  val emptyBody: Entity[F]


  val DefaultBatchRequest = HttpRequest(Method.PUT, "/$batch", emptyBody)

  /**
    * This does not handle the version tag + applyOptimisticConcurrency flag yet.
    */
  def toHeaders(o: Option[RO]): HttpHeaders = {
    val prefer = o.fold(EmptyHeaders)(roRenderer.render(_))
    prefer.map(str => HttpHeaders("Prefer"        -> str)).getOrElse(HttpHeaders.empty) ++
      o.user.map(u => HttpHeaders("MSCRMCallerId" -> u)).getOrElse(HttpHeaders.empty) ++
      (
        if (o.suppressDuplicateDetection) headers.SuppressDuplicateDetection
        else HttpHeaders.empty
      )
    //++ o.version.map(etag => HttpHeaders("If-None-Match" -> etag)).getOrElse(HttpHeaders.empty)
  }

  def mkGetListRequest(url: String, opts: Option[RO] = None) =
    HttpRequest[F](Method.GET, url, headers = toHeaders(opts), emptyBody)

  def mkCreateRequest[B](entitySet: String, body: B, opts: Option[RO] = None)(
      implicit e: EntityEncoder[F,B]) = {
    // HttpRequest(Method.POST, s"/$entitySet", body = Entity.fromString(body), headers = toHeaders(opts))
    val (xtras, ent) = e.toEntity(body)
    HttpRequest[F](Method.POST, s"/$entitySet", body = ent, headers = toHeaders(opts) ++ xtras)
  }

  /** Make a pure delete request. */
  def mkDeleteRequest(entitySet: String, keyInfo: ODataId, opts: Option[RO] = None) = {
    val etag =
      if (opts.applyOptimisticConcurrency.getOrElse(false) && opts.version.isDefined)
        HttpHeaders("If-Match" -> opts.version.get)
      else HttpHeaders.empty
    HttpRequest[F](Method.DELETE, s"/$entitySet(${keyInfo.render()})", headers = toHeaders(opts))
  }

  def mkGetOneRequest(url: String, opts: Option[RO] = None) = {
    val etag = opts.version.map(etag => HttpHeaders("If-None-Match" -> etag)).getOrElse(HttpHeaders.empty)
    HttpRequest[F](Method.GET, url, headers = toHeaders(opts) ++ etag, emptyBody)
  }

  def mkExecuteActionRequest[A](action: String,
    body: A,
    entitySetAndId: Option[(String, String)] = None,
    opts: Option[RO] = None)(
    implicit E: EntityEncoder[F, A]) = {
    val url = entitySetAndId.map { case (c, i) => s"/$c($i)/$action" }.getOrElse(s"/$action")
    val (hdrs, ent) = E.toEntity(body)
    HttpRequest[F](Method.POST, url, body = ent, headers = toHeaders(opts) ++ hdrs)
  }

  /**
    * Not sure adding $base to the @odata.id is absolutely needed. Probably is.
    * @see https://docs.microsoft.com/en-us/dynamics365/customer-engagement/developer/webapi/associate-disassociate-entities-using-web-api?view=dynamics-ce-odata-9
    */
  def mkAssociateRequest(fromEntitySet: String,
    fromEntityId: String,
    navProperty: String,
    toEntitySet: String,
    toEntityId: String,
    base: String,
    singleValuedNavProperty: Boolean = true): HttpRequest[F] = {
    val url  = s"/${fromEntitySet}(${fromEntityId})/$navProperty/$$ref"
    val body = s"""{"@odata.id": "$base/$toEntitySet($toEntityId)"}"""
    val method =
      if (singleValuedNavProperty) Method.PUT
      else Method.POST
    val (xtras, ent) = EntityEncoder[F,String].toEntity(body)
    HttpRequest(method, url, body = ent)
  }

  /**
    * Provide `to` if its a collection-valued navigation property, otherwise it
    * removes a single-valued navigation property.
    *
    * @see https://docs.microsoft.com/en-us/dynamics365/customer-engagement/developer/webapi/associate-disassociate-entities-using-web-api?view=dynamics-ce-odata-9
    */
  def mkDisassocatiateRequest(fromEntitySet: String,
    fromEntityId: String,
    navProperty: String,
    toId: Option[String]): HttpRequest[F] = {
    val navPropertyStr = toId.map(id => s"$navProperty($id)").getOrElse(navProperty)
    val url            = s"/$fromEntitySet($fromEntityId)/$navPropertyStr/$$ref"
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
    options: Option[RO] = None,
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
      if (options.applyOptimisticConcurrency.getOrElse(false) && options.version.isDefined)
        HttpHeaders("If-Match" -> options.version.get)
      else
        HttpHeaders.empty
    val mustHave = HttpHeaders.empty ++ Map("Content-Type" -> Seq("application/json", "type=entry"))
    HttpRequest(Method.PATCH,
                s"${base.getOrElse("")}/$entitySet($id)",
                toHeaders(options) ++ h1 ++ h2 ++ h3 ++ xtra ++ mustHave,
      ent)
  }

  def mkExecuteFunctionRequest(function: String,
    parameters: Map[String, scala.Any] = Map.empty,
    entity: Option[(String, String)] = None) = {
    val q: Seq[(String, String)] = parameters.keys.zipWithIndex
      .map(x => (x._1, x._2 + 1))
      .map {
        case (k, i) =>
          parameters(k) match {
            case s: String => (s"$k=@p$i", s"@p$i='$s'")
            case x @ _     => (s"$k=@p$i", s"@p$i=$x")
          }
      }
      .toSeq

    val pvars        = q.map(_._1).mkString(",")
    val pvals        = (if (q.size > 0) "?" else "") + q.map(_._2).mkString("&")
    val functionPart = s"/$function($pvars)$pvals"

    val entityPart = entity.map(p => s"/${p._1}(${p._2})").getOrElse("")

    val url = s"$entityPart$functionPart"
    HttpRequest[F](Method.GET, url, body=emptyBody)
  }

  /** @depecated. Use `mkBatch`. */
  def mkBatchRequest[A](headers: HttpHeaders, m: Multipart[F]): HttpRequest[F] = mkBatch(m, headers)

  /**
    * Body in HttpRequest is ignored and is instead generated from m.
    * Since the parts will have requests, you need to ensure that the
    * base URL used in those requests have a consistent base URL.
    */
  def mkBatch(m: Multipart[F], headers: HttpHeaders = HttpHeaders.empty): HttpRequest[F] = {
    val (xtras, ent) = EntityEncoder[F, Multipart[F]].toEntity(m)
    HttpRequest(Method.POST, "/$batch", headers = headers ++ xtras, body = ent)
  }
}
