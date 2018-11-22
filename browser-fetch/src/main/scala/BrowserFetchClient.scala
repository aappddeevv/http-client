// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg.odata.client
package http

import scala.scalajs.js
import org.scalajs.dom
import dom.experimental._
import scala.concurrent.{Future, ExecutionContext}

import cats._
import cats.data._
import cats.implicits._
import cats.effect._
import js.JSConverters._

object BrowserFetch {

  implicit val headersSemigroup = 
    new Semigroup[Headers] {
      def combine(lhs: Headers, rhs: Headers): Headers = {
        val result = new Headers(lhs)
        for((key,value) <- rhs) result.set(k, v)
        result
    }
}

/**
 * Client based on browser `fetch`.
 * 
 * @see https://developer.mozilla.org/en-US/docs/Web/API/WindowOrWorkerGlobalScope/fetch
 */
object BrowserFetchClient extends LazyLogger {
  import BrowserFetch._

  def create[F[_]](dataUrl: Option[String],
    baseRequestInit: Option[RequestInit] = None)(
      implicit ec: ExecutionContext,  F: MonadError[F, Throwable],
      PtoF: js.Promise ~> F,
      IOtoF: IO ~> F,
      PtoIO: js.Promise ~> IO): Client[F] = {

    val base = dataUrl.map(u => if(u.endsWith("/")) u.dropRight(1) else u).getOrElse("")

    val svc: HttpRequest[F] => F[Response[F]] = { request =>
      val hashttp = request.path.startsWith("http")
      assert(request.path(0) == '/' || hashttp, s"Request path must start with a slash (/) or http: ${request.path}}")
      val url                        = (if (!hashttp) base else "") + request.path
      IOtoF(request.body).flatMap { bodyString =>
        val fetchopts = 
          baseRequestInit.getOrElse(RequestInit()).toJSDictionary ++
        RequestInit(
          body = bodyString,
          headers = headersSemigroup.combine(
            baseRequestInit.map(_.headers).getOrElse(Headers()),
            req.headers.mapValues(_.mkString(";")).toJSDictionary),
          method = request.method.name
        ).toJSDictionary
        PtoF(fetch(url, fetchopts)).attempt.flatMap {
          case Right(r) =>
            // convert headers as String -> Seq[String] to just String -> String, is this wrong?
            val headers: HttpHeaders = r.headers.raw().mapValues(_.toSeq).toMap
            //.asInstanceOf[js.Dictionary[Seq[String]]]
            val hresp = HttpResponse[F](Status.lookup(r.status), headers, PtoIO(r.text()))
            F.pure(hresp)
          case Left(e) =>
            F.raiseError(CommunicationsFailure("browser node fetch client", Some(e)))
        }
      }
    }
    Client(svc)
  }
}
