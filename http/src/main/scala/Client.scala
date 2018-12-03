// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg.odata.client
package http

import fs2.Stream
import cats._
import cats.data._
import cats.implicits._

/**
 * A thin layer over a HTTP service that takes `HttpRequest => F[HttpResonse]`
 * and that adds implcit convenience for finding response decoders and handling
 * unsuccessful (non-200 range) responses. A higher level client may provide
 * more nuanced error handling. Since this layer can detect an unexpected status
 * and error on that condition, implementations must provide that method.
 * 
 * Based directly on http4s design. It's really a `Kleisli. `Client` is really a
 * wrapper around a function `Request => F[Response]`.
 */
trait Client[F[_]] {
  def run(req: HttpRequest[F]): F[HttpResponse[F]]
  /** Process response regardless of status. */
  def fetch[A](request: HttpRequest[F])(f: HttpResponse[F] => F[A]): F[A]
  /** Process response regardless of status. */
  def fetch[A](request: F[HttpRequest[F]])(f: HttpResponse[F] => F[A]): F[A]

  def expectOr[A](req: HttpRequest[F])(onError: (HttpRequest[F], HttpResponse[F]) => F[Throwable])(
    implicit d: EntityDecoder[F, A]): F[A]
  def expectOr[A](req: F[HttpRequest[F]])(onError: (HttpRequest[F], HttpResponse[F]) => F[Throwable])(
    implicit d: EntityDecoder[F, A]): F[A]

  /** Expect a successful response. */
  def expect[A](req: HttpRequest[F])(implicit d: EntityDecoder[F, A]): F[A]
  /** Expect a successful response. */
  def expect[A](req: F[HttpRequest[F]])(implicit d: EntityDecoder[F, A]): F[A]

  /**
   * Fetch response, process response with a decoder `d` regardless of status,
   * unlike `expect` which expects a succesful response.
   */
  def fetchAs[A](req: HttpRequest[F])(implicit d: EntityDecoder[F, A]): F[A]
  /** Fetch and process with decoder regardless of status. */
  def fetchAs[A](req: F[HttpRequest[F]])(implicit d: EntityDecoder[F, A]): F[A]

  /** Fetch and return the status. */
  def status(req: HttpRequest[F]): F[Status]
  /** Fetch and return the status. */
  def status(req: F[HttpRequest[F]]): F[Status]

  /** Fetch response and process as a stream. */
  def stream[A](req: HttpRequest[F]): Stream[F, HttpResponse[F]]
  def streaming[A](req: HttpRequest[F])(f: HttpResponse[F] => Stream[F, A]): Stream[F, A]
  def streaming[A](req: F[HttpRequest[F]])(f: HttpResponse[F] => Stream[F, A]): Stream[F, A]

  /** Submits a request and returns true if and only if the response status is
   * successful
   */
  def successful(req: HttpRequest[F]): F[Boolean]
  def successful(req: F[HttpRequest[F]]): F[Boolean]

  def toKleisli[A](f: HttpResponse[F] => F[A]): Kleisli[F, HttpRequest[F], A]
}

object Client {
  /** Create a new Client. Specificy how to raise an unexpected status error. */
  def apply[F[_]](f: HttpRequest[F] => F[HttpResponse[F]],
    mkStatusError: (HttpRequest[F], HttpResponse[F]) => F[Throwable])(
    implicit M: MonadError[F,Throwable]): Client[F] =
    new DefaultClient[F] {
      def run(req: HttpRequest[F]) = f(req)
      def defaultOnError(req: HttpRequest[F], resp: HttpResponse[F]) =
        mkStatusError(req,resp)
    }

  /**
   * Use the super simple `UnexpectedHttpStatus` exception for the status error.
   * Unless you are only using the HTTP layer, you'll want to customize a client
   * with your own unexpected status exception.
   */
  def apply[F[_]](f: HttpRequest[F] => F[HttpResponse[F]])(
    implicit M: MonadError[F,Throwable]): Client[F] =
    new DefaultClient[F] {
      def run(req: HttpRequest[F]) = f(req)
      def defaultOnError(req: HttpRequest[F], resp: HttpResponse[F]) =
        M.pure(new UnexpectedHttpStatus(resp.status))
    }

  /** Client that converts the request to a response directly with status OK. */
  def identity[F[_]](implicit M: MonadError[F, Throwable]): Client[F] =
    Client.apply(req => M.pure(HttpRequest.toResponse(req)))
}
