// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg
package client
package odata

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
 * 
 * Users of this client generally listen for errors the specific `http.Client`
 * generates as well as the unexpected status errors the odata layer can create.
 */
class BasicODataClient[F[_]](
  val http: client.http.Client[F,Throwable],
  val base: String,
  mkStatusError: (String, HttpRequest[F], HttpResponse[F]) => F[Throwable],
  reportError: F[Throwable] => F[Throwable]
)(implicit
  C: Stream.Compiler[F,F],
  M: MonadError[F,Throwable]
) extends ODataClient[F] {
  type PreferOptions = BasicPreferOptions
  type RequestOptions = BasicRequestOptions[PreferOptions]

  implicit protected val compiler = C
  val F = M
  private val prenderer = HeaderRenderer[PreferOptions]
  val optRenderer = HeaderRenderer.instance[RequestOptions](opts => prenderer(opts.prefers))

  def mkUnexpectedError[A](msg: String, req: HttpRequest[F], resp: HttpResponse[F]): F[A] =
    F.flatMap(reportError(mkStatusError(msg, req, resp)))(F.raiseError)
}

/**
 * Basic OData client.
 */
object BasicODataClient {

  /** Create a new `BasicODataClient` with a specific inner error. */
  def apply[
    F[_]: MonadError[?[_],Throwable],
    InnerE <: js.Object
  ](
    httpClient: client.http.Client[F, Throwable],
    baseUrl: String,
    mkUnexpectedStatus: (String, HttpRequest[F], HttpResponse[F]) => F[Throwable],
    reportError: F[Throwable] => F[Throwable]
  )(
    implicit C: Stream.Compiler[F,F]
  ): ODataClient[F] =
    new BasicODataClient[F](httpClient, baseUrl, mkUnexpectedStatus, reportError)

  /** Create an OData client layer unexpected status error. Use this for
   * `mkUnexpectedStatus` in `apply`. Creates instance of class
   * `odata.UnexpectedStatus`.
   */
  def mkUnexpectedStatus[
    F[_]: MonadError[?[_],Throwable],
    InnerE <: js.Object
  ](
    msg: String, req: HttpRequest[F], resp: HttpResponse[F]
  ): F[Throwable] =
    Monad[F].flatMap(resp.body.content){ str =>
      println(s"mkUnexpectedStatus: $str")
      val jsobj = common.Utils
        .parseJsonWithDates[ErrorResponse[ODataErrorType[InnerE]]](str)
      MonadError[F,Throwable].raiseError(
        new UnexpectedStatus[F, ODataErrorType[InnerE]](
          status = resp.status,
          request = Option(req),
          response = Option(resp),
          odata = Option(jsobj).flatMap(_.error.toOption),
          note = Option(msg))
      )}

  /** Create a new `BasicODataClient` with the inner error as js.Object and a
    * default error maker/reporter.
   */
  def apply[F[_]: MonadError[?[_],Throwable]](
    httpClient: client.http.Client[F,Throwable],
    baseUrl: String
  )(
    implicit C: Stream.Compiler[F,F]
  ): ODataClient[F] =
    new BasicODataClient[F](
      http = httpClient,
      base = baseUrl,
      mkStatusError = mkUnexpectedStatus[F,js.Object],
      reportError = identity)
}

