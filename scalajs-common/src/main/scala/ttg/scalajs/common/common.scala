// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg.scalajs

import scala.concurrent._
import scala.scalajs.js
import scalajs.runtime.wrapJavaScriptException
import js.|
import fs2._
import cats.~>
import cats.effect._

import scala.scalajs.runtime.wrapJavaScriptException

package object common {

  type JsAnyDict = js.Dictionary[js.Any]

  implicit def jsPromiseToIO[A](implicit ec: ExecutionContext): js.Promise[A] ~> IO[A] =
    new (js.Promise ~> IO) {
      override def apply(p: js.Promise[A]) =
        IO.async { cb =>
          p.`then`[Unit](
            { (v: A) => cb(Right(v))},
            js.defined { (e: scala.Any) =>
              cb(Left(wrapJavaScriptException(e)))
            }
          )
          () // return unit
        }
    }

  type Reviver = js.Function2[js.Any, js.Any, js.Any]
}
