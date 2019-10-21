// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg
package client
package http

import fs2.Stream
import cats._
import cats.data._
import cats.implicits._
import cats.effect._

private[http]
abstract class DefaultClient[B1[_], C1, B2[_], C2, F[_]: MonadError[?[_], E]:FlatMap, E <: Throwable]
    extends Client[B1,C1,B2,C2,F,E] {

  val F = MonadError[F, E]
  
  def fetch[A](request: HttpRequest[B1, C1])(f: HttpResponse[B2, C2] => F[A]): F[A] =
    run(request).flatMap(f)

  def fetch[A](request: F[HttpRequest[B1,C1]])(f: HttpResponse[B2,C2] => F[A]): F[A] =
    request.flatMap(fetch(_)(f))

  def fetchAs[A](request: HttpRequest[B1,C1])(implicit d: EntityDecoder[B2,C2,F,A]): DecodeResult[F,A] =
    // we unwrap then immediately rewrap, not efficient
    DecodeResult(run(request).flatMap(d.decode(_).value))

  def fetchAs[A](request: F[HttpRequest[B1,C1]])(implicit d: EntityDecoder[B2,C2,F,A]): DecodeResult[F,A] =
    // not efficient
    DecodeResult(request.flatMap(fetchAs[A](_)(d).value))

  def status(request: HttpRequest[B1,C1]): F[Status] =
    fetch(request)(resp => Monad[F].pure(resp.status))

  def status(request: F[HttpRequest[B1,C1]]): F[Status] = request.flatMap(status)

  def successful(req: HttpRequest[B1,C1]): F[Boolean] =
    status(req).map(_.isSuccess)

  def successful(req: F[HttpRequest[B1,C1]]): F[Boolean] =
    req.flatMap(successful)

  // def toKleisli[A](f: HttpResponse[F] => F[A]): Kleisli[F, HttpRequest[F], A] =
  //   Kleisli(fetch(_)(f))
}
