// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg
package scalajs
package common

import scala.scalajs.js
import js._
import js.|

final case class JsAnyOps(a: js.Any) {
  @inline def asJsObj: js.Object          = a.asInstanceOf[js.Object]
  @inline def asDyn: js.Dynamic           = a.asInstanceOf[js.Dynamic]
  @inline def asString: String            = a.asInstanceOf[String]
  @inline def asNumer: Number             = a.asInstanceOf[Number]
  @inline def asInt: Int                  = a.asInstanceOf[Int]
  @inline def asDouble: Double            = a.asInstanceOf[Double]
  @inline def asBoolean: Boolean          = a.asInstanceOf[Boolean]
  @inline def asJsArray[A]: js.Array[A]   = a.asInstanceOf[js.Array[A]]
  @inline def toJson: String              = js.JSON.stringify(a)
  @inline def asUndefOr[A]: js.UndefOr[A] = a.asInstanceOf[js.UndefOr[A]]
  @inline def toNonNullOption[T <: js.Any]: Option[T] = {
    // also defined in react package, repeated here
    if (js.isUndefined(a) || a == null) None
    else Option(a.asInstanceOf[T])
  }

  @inline def toTruthy: Boolean =
    js.DynamicImplicits.truthValue(a.asInstanceOf[js.Dynamic])
}

trait JsAnySyntax {
  implicit def jsAnyOpsSyntax(a: js.Any) = new JsAnyOps(a)
}
