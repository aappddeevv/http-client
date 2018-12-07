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
  F[_]: Monad: ErrorChannel[?[_],E],
  IE <: js.Object,
  E <: ODataException[F, ODataErrorType[IE]]
](
  val http: client.http.Client[F,E],
  val base: String,
  val mkStatusError: (String, HttpRequest[F], HttpResponse[F]) => F[E]
)(
  implicit C: Stream.Compiler[F,F]
) extends ODataClient[F, E] {
  type PreferOptions = BasicPreferOptions
  type RequestOptions = BasicRequestOptions[PreferOptions]

  implicit protected val compiler = C
  val F = Monad[F]
  val EC = ErrorChannel[F,E]
  private val prenderer = HeaderRenderer[PreferOptions]
  val optRenderer = HeaderRenderer.instance[RequestOptions](opts => prenderer(opts.prefers))

  def mkUnexpectedError[A](msg: String, req: HttpRequest[F], resp: HttpResponse[F]): F[A] =
    F.flatMap(mkStatusError(msg, req, resp))(EC.raise)
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
    F[_]: Monad: ErrorChannel[?[_],E],
    InnerE <: js.Object,
    E <: ODataException[F, ODataErrorType[InnerE]]
  ](
    httpClient: client.http.Client[F,E],
    baseUrl: String,
    mkUnexpectedStatus: (String, HttpRequest[F], HttpResponse[F]) => F[E]
  )(
    implicit C: Stream.Compiler[F,F]
  ): ODataClient[F,E] =
    new BasicODataClient[F, InnerE, E](httpClient, baseUrl, mkUnexpectedStatus)

  /** Create an OData client layer unexpected status error. Use this for
   * `mkUnexpectedStatus` in `apply`.
   */
  def mkUnexpectedStatus[
    F[_]: Monad: ErrorChannel[?[_],E],
    InnerE <: js.Object,
    E <: ODataException[F, ODataErrorType[InnerE]]
  ](
    msg: String, req: HttpRequest[F], resp: HttpResponse[F]
  ): F[E] =
    Monad[F].flatMap(resp.body.content){ str =>
      val jsobj = common.Utils
        .parseJsonWithDates[ErrorResponse[ODataErrorType[InnerE]]](str)
      ErrorChannel[F,E].raise(
        new UnexpectedStatus[F, ODataErrorType[InnerE]](
          status = resp.status,
          request = Option(req),
          response = Option(resp),
          odata = Option(jsobj).flatMap(_.error.toOption),
          note = Option(msg)).asInstanceOf[E]
      )}

  /** Set the inner error type explicitly to `js.Object`. */
  type DefaultThrowableType[F[_]] = ODataException[F, ODataErrorType[js.Object]]

  /** Create a new `BasicODataClient` with the inner error as js.Object and a default error maker. */
  def apply[
    F[_]: Monad: ErrorChannel[?[_],E],
    E <: DefaultThrowableType[F]
  ](
    httpClient: client.http.Client[F, E], baseUrl: String
  )(
    implicit C: Stream.Compiler[F,F]
  ): ODataClient[F,E] =
    new BasicODataClient[F,js.Object,E](httpClient, baseUrl, mkUnexpectedStatus[F,js.Object,E])
}

