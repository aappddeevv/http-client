// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg.scalajs

import scala.concurrent._
import scala.scalajs.js
import scalajs.runtime.wrapJavaScriptException
import fs2._
import cats.~>
import cats.effect._

import scala.scalajs.runtime.wrapJavaScriptException

/**
 * Common defs.
 * 
 * @todo: Define LiftIO[js.Promise]
 */
package object common {

  type JsAnyDict = js.Dictionary[js.Any]

  def jsPromiseToIO[A](implicit ec: ExecutionContext):
      js.Promise ~> IO =
    new (js.Promise ~> IO) {
      override def apply[A](p: js.Promise[A]) =
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

  def jsPromiseToF[F[_], A](
    implicit F: Async[F]):
      js.Promise ~> F =
    new (js.Promise ~> F) {
      override def apply[A](p: js.Promise[A]) =
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

  type Reviver = js.Function2[js.Any, js.Any, js.Any]

  /** js regex for a date from an OData server. */
  val dateRegex =
    new js.RegExp("""^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2}(?:\.\d*)?)Z$""")

  /** JSON reviver that matches nothing. */
  val undefinedReviver = js.undefined.asInstanceOf[Reviver]

  /** JSON Date reviver based on ISO string format `dateRegex`. (js) */
  val dateReviver: Reviver =
    (key, value) => {
      if (js.typeOf(value) == "string") {
        val a = dateRegex.exec(value.asInstanceOf[String])
        if (a != null)
          new js.Date(
            js.Date.UTC(
              a(1).get.toInt,
              a(2).get.toInt - 1,
              a(3).get.toInt,
              a(4).get.toInt,
              a(5).get.toInt,
              a(6).get.toInt
            ))
        else value
      } else value
    }
}
