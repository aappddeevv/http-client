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
class BasicODataClient[
  F[_]: MonadError[?[_],Throwable],
  IE <: js.Object
](
  val http: client.http.Client[F],
  val base: String,
  val mkStatusError: (String, HttpRequest[F], HttpResponse[F]) => F[Throwable]
)(
  implicit C: Stream.Compiler[F,F]
) extends ODataClient[F] {
  type PreferOptions = BasicPreferOptions
  type RequestOptions = BasicRequestOptions[PreferOptions]

  implicit protected val compiler = C
  val F = MonadError[F, Throwable]
  private val prenderer = HeaderRenderer[PreferOptions]
  val optRenderer = HeaderRenderer.instance[RequestOptions](opts => prenderer(opts.prefers))

  def mkUnexpectedError[A](msg: String, req: HttpRequest[F], resp: HttpResponse[F]): F[A] =
    F.flatMap(mkStatusError(msg, req, resp))(F.raiseError)
}

/**
 * Basic OData client.
 * 
 * Note thhat the error type is a lie. You currently could have a Throwable
 * bubble up througuh this layer. What should we do?
 */
object BasicODataClient {

  /** Create a new `BasicODataClient`. */
  def apply[
    F[_]: MonadError[?[_],Throwable],
    InnerE <: js.Object
  ](
    httpClient: client.http.Client[F],
    baseUrl: String,
    mkUnexpectedStatus: (String, HttpRequest[F], HttpResponse[F]) => F[Throwable]
  )(
    implicit C: Stream.Compiler[F,F]
  ): ODataClient[F] =
    new BasicODataClient[F, InnerE](httpClient, baseUrl, mkUnexpectedStatus)

  /** Create an OData client layer unexpected status error. Use this for
   * `mkUnexpectedStatus` in `apply`.
   */
  def mkUnexpectedStatus[
    F[_]: MonadError[?[_],Throwable],
    InnerE <: js.Object
  ](
    msg: String, req: HttpRequest[F], resp: HttpResponse[F]
  ): F[Throwable] =
    Monad[F].flatMap(resp.body.content){ str =>
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

  /** Create a new `BasicODataClient` with the inner error as js.Object and a default error maker. */
  def apply[
    F[_]: MonadError[?[_],Throwable]
  ](
    httpClient: client.http.Client[F],
    baseUrl: String
  )(
    implicit C: Stream.Compiler[F,F]
  ): ODataClient[F] =
    new BasicODataClient[F,js.Object](httpClient, baseUrl, mkUnexpectedStatus[F,js.Object])
}

