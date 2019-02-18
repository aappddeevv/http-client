// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg
package scalajs
package common

import scala.scalajs.js
import js._
import fs2._
import scala.concurrent._
import duration._
import java.util.concurrent.{TimeUnit => TU}
import cats._
import cats.data._
import cats.implicits._
import cats.effect._

object fs2helpers {

  /**
   * From cats-effect gitter channel. This does not use fs2 at all actually and
   * does not work from a queue so its incrementally parallel.
   */
  //def parallelWithLimit[A](limit: Int, as: List[IO[A]]): IO[List[A]] =
    //as.grouped(limit).toList.flatTraverse(_.parSequence)


  /** Given a list of `F`s, eval `n` at a time. Output order may not be preserved.
   */
  def evalN[F[_], A](fs: Seq[F[A]], n: Int = 5)(
    implicit F: ConcurrentEffect[F]): F[Vector[A]] =
    //Stream.emits(fs).flatMap(Stream.eval(_)).compile.toVector
    Stream.emits(fs).covary[F].map(Stream.eval(_)).parJoin(n).compile.toVector

  /** Use it with `Stream.through`. */
  def liftToPipe[A, B](f: A => IO[B]): Pipe[IO, A, B] = _ evalMap f

  /** Lift f to Sink. Use with `Stream.observe`. */
  def liftToSink[A](f: A => IO[Unit]): Sink[IO, A] = liftToPipe[A, Unit](f)

  /** Log something. Runs f in a IO. */
  def log[A](f: A => Unit) = liftToPipe { a: A =>
    IO { f(a); a }
  }

  /** A pipe that given a stream, delays it by delta. */
  def sleepFirst[F[_], O](
      delta: FiniteDuration)(implicit T: Timer[F]): Pipe[F, O, O] =
    delayme => Stream.sleep(delta) >> delayme

  /**
    * Unfold, periodically checking an effect for new values.  Time between
    * checks is obtained using getDelay potentially using the returned effect
    * value. f is run immediately when the stream runs.
    *
    * @param A Output element type.
    * @param f Call an effect to get a value.
    * @param getDelay Extract amount to delay from that value.
    */
  def unfoldEvalWithDelay[F[_], A](f: => F[Option[A]], getDelay: A => FiniteDuration)(
    implicit M: Functor[F], F: Async[F], T: Timer[F]): Stream[F, A] =
    Stream.unfoldEval(0.seconds) { delay =>
      M.map(T.sleep(delay) *> f) { opt =>
        //M.map(s.effect.delay(f, delay)) { opt =>
        opt.map { a =>
          (a, getDelay(a))
        }
      }
    }

  /**
    * Calculate a delay but use fraction to shorten the delay.
    */
  def shortenDelay(delay: FiniteDuration, fraction: Double = 0.95): FiniteDuration =
    FiniteDuration((delay * fraction).toSeconds, TU.SECONDS)

  /**
    * Given an effect F, wait until the criteria stop is met.
    */
  def pollWait[F[_], A](f: => F[A], stop: A => Boolean, poll: FiniteDuration = 10.seconds)(
      implicit T: Timer[F],
      F: Async[F],
      ec: ExecutionContext): Stream[F, A] = {
    unfoldEvalWithDelay[F, A]({
      f.map(a => (stop(a), a)).map { case (stopflag, a) => if (stopflag) None else Some(a) }
    }, _ => poll)
  }

  /** Throttle a stream. */
  def throttle[F[_], A](
      delay: FiniteDuration = 5.millis)(implicit T: Timer[F], ec: ExecutionContext, F: Effect[F]): Pipe[F, A, A] =
    _.zipLeft(Stream.awakeEvery[F](delay))

}
