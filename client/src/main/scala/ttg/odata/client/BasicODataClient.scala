// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg.odata
package client

import scala.scalajs.js
import cats._
import cats.implicits._
import fs2._

import ttg.scalajs.common
import http._
import HeaderRenderer._

/**
 * Simple OData client based on the contents of this package. You can use this
 * directly for simple OData client needs based on a generic OData server. You
 * should customize raising an error as the default strategy is fairly
 * unhelpful. You will want to use a http client that uses the
 * `UnexpectedStatus` class from this package so use `BasicHttpClient` as the
 * innermost middleware.
 */
class BasicODataClient[F[_]](
  val http: client.http.Client[F],
  val base: String,
  val handleUnexpectedStatus: (HttpResponse[F], String, Option[HttpRequest[F]]) => F[Throwable])(
  implicit M: MonadError[F, Throwable], C: Stream.Compiler[F,F]
) extends ODataClient[F] {
  type PreferOptions = BasicPreferOptions
  type RequestOptions = BasicRequestOptions[PreferOptions]

  implicit protected val compiler = C
  implicit val F = M
  private val prenderer = HeaderRenderer[PreferOptions]
  val optRenderer = HeaderRenderer.instance[RequestOptions](opts => prenderer(opts.prefers))

  def raiseError[A](resp: HttpResponse[F], msg: String, req: Option[HttpRequest[F]]): F[A] =
    M.flatMap(handleUnexpectedStatus(resp, msg, req))(F.raiseError)
}

object BasicODataClient {

  /** Create a new `SimpleODataClient`. */
  def apply[F[_]](httpClient: client.http.Client[F], baseUrl: String,
    handleUnexpectedStatus: (HttpResponse[F], String, Option[HttpRequest[F]]) => F[Throwable])(
    implicit M: MonadError[F, Throwable], C: Stream.Compiler[F,F]): ODataClient[F] =
    new BasicODataClient[F](httpClient, baseUrl, handleUnexpectedStatus)

  /** Break out the OData payload error if it exist and map into `SimpleUnexpectedStatus`. */
  def handleUnexpectedStatus[F[_], IE <: js.Object](
    resp: HttpResponse[F], msg: String, req: Option[HttpRequest[F]])(
    implicit M: MonadError[F,Throwable]): F[Throwable] =
    M.flatMap(resp.body.content){ str =>
      val jsobj = common.Utils.parseJsonWithDates[ErrorResponse[ErrorResponseDetail[IE, CodeMessageTarget]]](str)
      M.raiseError(
        SimpleUnexpectedStatus(
          status = resp.status,
          request = req,
          response = Option(resp),
          odataError = Option(jsobj).flatMap(_.error.toOption),
          note = Option(msg))
      )}

  /** Create a new `SimpleODataClient` with default error handling. */
  def apply[F[_]](httpClient: client.http.Client[F], baseUrl: String)(
    implicit M: MonadError[F, Throwable], C: Stream.Compiler[F,F]): ODataClient[F] =
    new BasicODataClient[F](httpClient, baseUrl, handleUnexpectedStatus[F, js.Object])
}

