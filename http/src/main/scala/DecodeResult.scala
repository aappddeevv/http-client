// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg
package client
package http

import scala.scalajs.js
import cats._
import cats.data._
import cats.effect._
import cats.implicits._

/**
  * DecodeResult smart constructors.
  */
object DecodeResult {

  /** Create a DecodeResult directly from the effectful Either. */
  def apply[F[_], A](fa: F[Either[DecodeFailure, A]]): DecodeResult[F, A] = EitherT(fa)

  /** Lift an effectful A to a DecodeResult. */
  def success[F[_]: Functor, A](a: F[A]): DecodeResult[F, A] =
    DecodeResult(a.map(Either.right(_)))

  /** Lift a pure A to a DecodeResult. */
  def success[F[_]: Applicative, A](a: A): DecodeResult[F, A] = success(Applicative[F].pure(a))

  /** Lift an effectful DecodeFailure to a DecodeResult. */
  def failure[F[_]: Functor, A](e: F[DecodeFailure]): DecodeResult[F, A] =
    DecodeResult(e.map(Either.left(_)))

  /** Lift an plain DecodeFailure to a DecodeResult. */
  def failure[F[_]: Applicative, A](e: DecodeFailure): DecodeResult[F, A] =
    failure(Applicative[F].pure(e))

  /** Return a failure. */
  def fail[F[_]: Applicative, A]: DecodeResult[F, A] =
    failure(MessageBodyFailure("Intentionally failed."))
}
