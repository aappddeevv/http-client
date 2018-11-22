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
 * unsuccessful (non-200 range) responses.
 * 
 * Based directly on http4s design. It's really a Kleisli.
 */
trait Client[F[_]] {
  def run(req: HttpRequest[F]): F[HttpResponse[F]]
  /** Process response regardless of status. */
  def fetch[A](request: HttpRequest[F])(f: HttpResponse[F] => F[A]): F[A]
  /** Process response regardless of status. */
  def fetch[A](request: F[HttpRequest[F]])(f: HttpResponse[F] => F[A]): F[A]

  def expectOr[A](req: HttpRequest[F])(onError: HttpResponse[F] => F[Throwable])(
    implicit d: EntityDecoder[F, A]): F[A]
  def expectOr[A](req: F[HttpRequest[F]])(onError: HttpResponse[F] => F[Throwable])(
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
  /** Create a new Client. */
  def apply[F[_]](f: HttpRequest[F] => F[HttpResponse[F]])(implicit M: MonadError[F,Throwable]): Client[F] =
    new DefaultClient[F] {
      def run(req: HttpRequest[F]): F[HttpResponse[F]] = f(req)
    }

  /**
   * Convert an `HttpResponse` to an `UnexpectedStatus` error if the status test
   * returns true. Callers can use this function if they are not sure that an
   * effect carrying a `HttpResponse` has converted the effect to contain an
   * error for specific status codes.
   */
  def errorIfUnexpectedStatus[F[_]](isError: Status => Boolean, request: Option[HttpRequest[F]])(
    implicit F: ApplicativeError[F, Throwable]): HttpResponse[F] => F[HttpResponse[F]] =
    response => {
      if (!isError(response.status)) F.pure(response)
      else F.raiseError(UnexpectedStatus(response.status, request, Some(response)))
    }
}

private[client]
abstract class DefaultClient[F[_]](implicit F: MonadError[F, Throwable]) extends Client[F] {

  def fetch[A](request: HttpRequest[F])(f: HttpResponse[F] => F[A]): F[A] =
    run(request).flatMap(f)

  def fetch[A](request: F[HttpRequest[F]])(f: HttpResponse[F] => F[A]): F[A] =
    request.flatMap(fetch(_)(f))

  def expectOr[A](req: HttpRequest[F])(onError: HttpResponse[F] => F[Throwable])(
      implicit d: EntityDecoder[F, A]): F[A] = {
    fetch(req) {
      case Status.Successful(resp) =>
        d.decode(resp).fold(throw _, identity)
      case failedResponse =>
        onError(failedResponse).flatMap(F.raiseError)
    }
  }

  def expectOr[A](req: F[HttpRequest[F]])(onError: HttpResponse[F] => F[Throwable])(
    implicit d: EntityDecoder[F, A]): F[A] =
    req.flatMap(expectOr(_)(onError))
  
  def expect[A](req: HttpRequest[F])(implicit d: EntityDecoder[F, A]): F[A] =
    expectOr(req)(defaultOnError)

  def expect[A](req: F[HttpRequest[F]])(implicit d: EntityDecoder[F, A]): F[A] =
    expectOr(req)(defaultOnError)

  def fetchAs[A](req: HttpRequest[F])(implicit d: EntityDecoder[F, A]): F[A] =
    fetch(req) { resp =>
      d.decode(resp).fold(throw _, identity)
    }

  def fetchAs[A](req: F[HttpRequest[F]])(implicit d: EntityDecoder[F, A]): F[A] =
    req.flatMap(fetchAs(_)(d))

  def status(req: HttpRequest[F]): F[Status] =
    fetch(req)(resp => F.pure(resp.status))

  def status(req: F[HttpRequest[F]]): F[Status] = req.flatMap(status)

  def stream[A](req: HttpRequest[F]): Stream[F, HttpResponse[F]] =
    Stream.eval(run(req))

  @deprecated("Use stream", "0.1.0")
  def streaming[A](req: HttpRequest[F])(f: HttpResponse[F] => Stream[F, A]): Stream[F, A] =
    stream(req).flatMap(f)

  @deprecated("Use stream", "0.1.0")
  def streaming[A](req: F[HttpRequest[F]])(f: HttpResponse[F] => Stream[F, A]): Stream[F, A] =
    Stream.eval(req).flatMap(stream).flatMap(f)

  def successful(req: HttpRequest[F]): F[Boolean] =
    status(req).map(_.isSuccess)

  def successful(req: F[HttpRequest[F]]): F[Boolean] =
    req.flatMap(successful)

  private def defaultOnError(resp: HttpResponse[F])(implicit F: Applicative[F]): F[Throwable] =
    F.pure(UnexpectedStatus(resp.status))

  def toKleisli[A](f: HttpResponse[F] => F[A]): Kleisli[F, HttpRequest[F], A] =
    Kleisli(fetch(_)(f))
}
