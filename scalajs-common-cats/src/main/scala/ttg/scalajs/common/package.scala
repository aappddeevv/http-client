// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg
package scalajs
package common

import scala.concurrent._
import scala.scalajs.js
import scala.scalajs.runtime.wrapJavaScriptException
import fs2._
import _root_.cats._
import _root_.cats.implicits._
import _root_.cats.effect._

/**
 * Common defs.
 * 
 * @todo: Define LiftIO[js.Promise]
 */
package object cats {

  /** Natural tranformation js.Promise to F using a cats Async[F] */
  def jsPromiseToF[F[_]](implicit F: Async[F]): js.Thenable ~> F =
    new (js.Thenable ~> F) {
      override def apply[A](p: js.Thenable[A]) =
        F.async { cb =>
          p.`then`[Unit](
            { (v: A) => cb(Right(v))},
            js.defined { (e: scala.Any) =>
              cb(Left(wrapJavaScriptException(e)))
            }
          )
          () // return unit
        }
    }

  /** Convert an IO to IO === identity natural transformation. */
  val ioToIO: IO ~> IO = new (IO ~> IO) {
    override def apply[A](ioa: IO[A]) = ioa
  }

  /** Natural transformation identity monad to IO. */
  val idToIO: Id ~> IO = new (Id ~> IO) {
    override def apply[A](ida: Id[A]): IO[A] = IO.pure(ida)
  }
}
