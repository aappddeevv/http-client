// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg.odata
package client

import cats._
import cats.implicits._
import fs2._

import http._
import HeaderRenderer._

/**
 * Simple OData client based on the contents of this package. You can use this
 * directly for simple OData client needs based on a generic OData server. You
 * should customize raising an error as the default strategy is fairly
 * unhelpful. To customize error handling simply subclass and override
 * raiseError. You will want to use a http client that uses the
 * `UnexpectedStatus` class from this package.
 */
class SimpleODataClient[F[_]](
  val http: client.http.Client[F],
  val base: String)(
  implicit M: MonadError[F, Throwable], C: Stream.Compiler[F,F]
) extends ODataClient[F] {
  type PreferOptions = BasicPreferOptions
  type RequestOptions = BasicRequestOptions[PreferOptions]

  implicit protected val compiler = C
  implicit val F = M
  private val prenderer = HeaderRenderer[PreferOptions]
  val optRenderer = HeaderRenderer.instance[RequestOptions](opts => prenderer(opts.prefers))

  /** An operation failed with an unexpected status, create an error. 
   * 
   * @todo: Extract out the odata error, if it exists.
   */
  def raiseError[A](resp: HttpResponse[F], msg: String, req: Option[HttpRequest[F]]): F[A] = {
    F.flatMap(resp.body.content){ str =>
      F.raiseError(
        SimpleUnexpectedStatus(
          status = resp.status,
          request = req,
          response = Option(resp))
      )}
  }
}

object SimpleODataClient {

  def apply[F[_]](httpClient: client.http.Client[F], baseUrl: String)(
    implicit M: MonadError[F, Throwable], C: Stream.Compiler[F,F]): ODataClient[F] =
    new SimpleODataClient[F](httpClient, baseUrl)
}

