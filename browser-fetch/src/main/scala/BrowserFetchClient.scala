// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg.odata
package client
package browserfetch

import scala.scalajs.js
import org.scalajs.dom
import dom.experimental._
import js.Dynamic.{global => g, newInstance => jsnew, literal => jsobj}

import cats._
import cats.data._
import cats.implicits._
import cats.effect._
import js.JSConverters._

import client.http._
import ttg.scalajs.common.implicits._

/** Fix some of the header conversions...need to use getAll. */
object ClientUtils {

  /** Combine headers, return a newly allocated Header. `append` vs `set` is used.
   */
  implicit val headersSemigroup = 
    new Semigroup[Headers] {
      def combine(lhs: Headers, rhs: Headers): Headers = {
        val result = new Headers(lhs)
        for(kvarray <- rhs) result.append(kvarray(0), kvarray(1))
        result
      }}

  /** Zero for Headers. Treat as immutable even though it's not. */
  lazy val emptyHeaders: Headers = new Headers()

  /** Convert HtppHeaders to Headers. */
  def toFetchHeaders(h: HttpHeaders): Headers = {
    val r = new Headers()
    for((k,v) <- h) r.set(k,v.mkString(","))
    r
  }

  def toHttpHeaders(h: Headers): HttpHeaders =
    HttpHeaders(h.map(entry => (entry(0), entry(1))).toSeq:_*)
    //HttpHeaders(h.map(entry => (entry(0), entry.drop(1).toSeq)).toSeq:_*)

  def toHeaders(h: HeadersInit): Headers =
    h match {
      case headers@_ if headers.isInstanceOf[Headers] => headers.asInstanceOf[Headers]
      case headers@_ if js.typeOf(headers.asInstanceOf[js.Any]) == "object" =>
        val r = new Headers()
        for((k,v) <- headers.asInstanceOf[js.Dictionary[String]]) r.set(k,v)
        r
      case headers@_ if js.Array.isArray(headers.asInstanceOf[js.Any]) =>
        val r = new Headers()
        for(a <- headers.asInstanceOf[js.Array[js.Array[String]]]) r.set(a(0), a(1))
        r
      case _ => new Headers()
    }
}

/**
 * Client based on browser `fetch`. Why is dataUrl an option? Are we accessing
 * the window document's location URL for a default?
 * 
 * @see https://developer.mozilla.org/en-US/docs/Web/API/WindowOrWorkerGlobalScope/fetch
 */
object Client { 
  import ClientUtils._

  /** Default error maker creates a `CommunicationsFailure`. */
  def mkError(msg: String, e: Option[Throwable]) =
    new CommunicationsFailure(msg, e)

  /** 
   * @tparam F `MonadError` is needed for `attempt` but all errors raised through `ErrorChannel`.
   * @tparam E Error type if `fetch` results in an error.
   */
  def apply[F[_]](
    dataUrl: Option[String],
    baseRequestInit: Option[RequestInit] = None,
    mkError: ((String, Option[Throwable]) => Throwable) = mkError
  )(
    implicit A: Async[F], M: MonadError[F, Throwable]
  ): http.Client[F] = {
    val base = dataUrl.map(u => if(u.endsWith("/")) u.dropRight(1) else u).getOrElse("")
    val M = MonadError[F,Throwable]

    val svc: HttpRequest[F] => F[HttpResponse[F]] = { request =>
      val hashttp = request.path.startsWith("http")
      assert(request.path(0) == '/' || hashttp,
        s"Request path must start with a slash (/) or http: ${request.path}}")
      val url                        = (if (!hashttp) base else "") + request.path
      M.flatMap(request.body.content){ bodyString =>
        val fetchopts = 
          baseRequestInit.getOrElse(RequestInit()).asDict[js.Any] ++
        RequestInit(
          body = bodyString,
          headers = 
            baseRequestInit.flatMap(_.headers.map(toHeaders(_)).toOption).getOrElse(emptyHeaders) |+|
              toFetchHeaders(request.headers),
          method = request.method.name.toUpperCase.asInstanceOf[HttpMethod]
        ).asDict[js.Any]
        M.flatMap(
          M.attempt(
            Fetch.fetch(url, fetchopts.asInstanceOf[RequestInit]).toF[F])) {
          case Right(r) =>
            M.pure(
              HttpResponse[F](
                Status.lookup(r.status),
                toHttpHeaders(r.headers),
                Entity(r.text().toF[F])))
          case Left(e) =>
            M.raiseError(mkError(s"browser fetch error: ${e.getMessage()}", Option(e)))
        }
      }
    }
    http.Client(svc)
  }
}
