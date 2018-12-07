// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg.odata.client
package http

import fs2.Stream
import cats._
import cats.data._
import cats.implicits._

private[http]
abstract class DefaultClient[F[_]: ErrorChannel[?[_],E]: Monad,E <: Throwable]
extends Client[F, E] {

  def fetch[A](request: HttpRequest[F])(f: HttpResponse[F] => F[A]): F[A] =
    run(request).flatMap(f)

  def fetch[A](request: F[HttpRequest[F]])(f: HttpResponse[F] => F[A]): F[A] =
    request.flatMap(fetch(_)(f))

  def expectOr[A](req: HttpRequest[F])(onError: (HttpRequest[F], HttpResponse[F]) => F[E])(
      implicit d: EntityDecoder[F, A]): F[A] = {
    fetch(req) {
      case Status.Successful(resp) =>
        // same as resp.as[B] when using syntax
        d.decode(resp).fold(throw _, identity)
      case failedResponse =>
        onError(req, failedResponse).flatMap(ErrorChannel[F,E].raise)
    }
  }

  def expectOr[A](req: F[HttpRequest[F]])(onError: (HttpRequest[F], HttpResponse[F]) => F[E])(
    implicit d: EntityDecoder[F, A]): F[A] =
    Monad[F].flatMap(req)(expectOr(_)(onError))
  
  def fetchAs[A](req: HttpRequest[F])(implicit d: EntityDecoder[F, A]): F[A] =
    fetch(req) { resp =>
      d.decode(resp).fold(throw _, identity)
    }

  def fetchAs[A](req: F[HttpRequest[F]])(implicit d: EntityDecoder[F, A]): F[A] =
    req.flatMap(fetchAs(_)(d))

  def status(req: HttpRequest[F]): F[Status] =
    fetch(req)(resp => Monad[F].pure(resp.status))

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

  def toKleisli[A](f: HttpResponse[F] => F[A]): Kleisli[F, HttpRequest[F], A] =
    Kleisli(fetch(_)(f))
}
