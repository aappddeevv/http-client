// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg
package client
package http

import cats.ApplicativeError

/** Typeclass for raising errors more specific than just Throwable in the
 * ApplicativeError and MonadError type classes. `raise` is typically used to
 * lift an E error into an `F` or to flatMap into an `F[E]` to obtain a `F[A]`.
 */
trait ErrorChannel[F[_], +E <: Throwable] {
  // we could U :> E but then that's 2 type parameters at the call site
  def raise[A](e: E @scala.annotation.unchecked.uncheckedVariance): F[A]
}

object ErrorChannel {
  def apply[F[_], E <: Throwable](implicit ev: ErrorChannel[F, E]) = ev
}

trait ErrorChannelInstances {
  /** Create ErrorChannel from `ApplicativeError` automatically. */
  implicit def instance[F[_], E <: Throwable](implicit F: ApplicativeError[F,E]): ErrorChannel[F,E] =
    new ErrorChannel[F,E] {
      override def raise[A](e: E) = F.raiseError(e)
    }
}

final case class ErrorChannelOps[F[_]: ErrorChannel[?[_],E], E <: Throwable](e: E) {
  /** Call `myErrorInstance.raise` to lift it into F. */
  def raise[A]: F[A] = ErrorChannel[F,E].raise[A](e)
}

trait ErrorChannelOpsSyntax {
  implicit def errorChannelOpsSyntax[F[_]: ErrorChannel[?[_], E],E <: Throwable](e: E): ErrorChannelOps[F,E] =
    ErrorChannelOps(e)
}
