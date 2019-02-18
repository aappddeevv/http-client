// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg
package client
package browserfetch

import scala.scalajs.js
import org.scalajs.dom
import dom.experimental._

import cats._
import cats.data._
import cats.implicits._
import cats.effect._
import js.JSConverters._

import ttg.scalajs.common
import client.http._
import ttg.scalajs.common.syntax.jspromise._

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
  lazy val emptyRequestInit: RequestInit = RequestInit()

  /** Convert HtppHeaders to Headers. */
  def toFetchHeaders(h: HttpHeaders): Headers = {
    val r = new Headers()
    for((k,v) <- h) r.set(k,v.mkString(","))
    r
  }

  def toHttpHeaders(h: Headers): HttpHeaders =
    HttpHeaders(h.map(entry => (entry(0), entry(1))).toSeq:_*)

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

  def combine(lhs: RequestInit, rhs: RequestInit): RequestInit =
    common.Utils.merge[RequestInit](lhs, rhs)

}

/**
 * Client based on browser `fetch`.
 * 
 * @see https://developer.mozilla.org/en-US/docs/Web/API/WindowOrWorkerGlobalScope/fetch
 */
object Client { 
  import ClientUtils._

  /** Using `Async` forces the error type to Throwable. All exceptions are wrapped
   * with a `CommunicationsFailure` exception by default since the response
   * status is not observed. This converts the body to text using `Body.text()`
   * by default but it can be changed by adding a tag, `body = blob` to use
   * `Body.blob()`.
   */
  def run[F[_]: Async](
    mkError: (String, Throwable) => Throwable =
      (m,e) => new CommunicationsFailure(s"$m: ${e.getMessage()}",Option(e))
  )(
    request: F[dom.experimental.Response] // request generates a response
  ): F[HttpResponse[F]] = {
    val M = Async[F]
    M.flatMap(
      M.attempt(request)) {
      case Right(r) =>
        M.pure(
          HttpResponse[F](
            Status.lookup(r.status),
            toHttpHeaders(r.headers),
            Entity(r.text().toF[F])))
      case Left(e) =>
        M.raiseError(mkError("browser fetch error",e))
    }
  }

  /** Create client based on the browser's fetch function. If a url base is not
    * provided it defaults to the document's location. This method formulates
    * the browser fetch Request object. `run` runs the fetch and converts the
    * result to a `HttpResponse`. Using `run` you can customize your effects
    * and error type.
   * @param base Base URL appended to all requests.
   * @param run Run the input effect and create an output effect.
   * @param convert Convert js.Promise to F. You can use common.jsPromiseToF.
   * @param mkClient Given a Kleisli, make a `Client`. You an use `http.Client.apply`.
   * @tparam A FlatMap to map into the HttpRequest's body.
   * asynchronously.
   */
  def apply[F[_]](
    base: Option[String],
    run: F[dom.experimental.Response] => F[HttpResponse[F]],
    mkClient: (HttpRequest[F] => F[HttpResponse[F]]) => Client[F, Throwable],    
    convert: js.Promise ~> F,
    baseRequestInit: Option[RequestInit] = None
  )(
    implicit A: FlatMap[F]
  ): http.Client[F,Throwable] = {
    val url = base.map(u => if(u.endsWith("/")) u.dropRight(1) else u).getOrElse("")
    val svc: HttpRequest[F] => F[HttpResponse[F]] = { request =>
      val hashttp = request.path.startsWith("http")
      // this should not be assert() here, this is out of the error channel!
      assert(request.path(0) == '/' || hashttp,
        s"Request path must start with a slash (/) or http: ${request.path}}")
      val path                        = (if (!hashttp) url else "") + request.path
      A.flatMap(request.body.content){ bodyString =>
        // merge RequestInits taking care to merge headers correctly
        val fetchopts = combine(
          baseRequestInit.getOrElse[RequestInit](emptyRequestInit),
          RequestInit(
            body = if(request.method==Method.GET) js.undefined else bodyString,
            headers =
              baseRequestInit.flatMap(_.headers.map(toHeaders(_)).toOption).getOrElse(emptyHeaders) |+|
                toFetchHeaders(request.headers),
            method = request.method.asString.asInstanceOf[HttpMethod]
          ))
        run(convert(Fetch.fetch(path, fetchopts)))
      }
    }
    mkClient(svc)
  }
}
