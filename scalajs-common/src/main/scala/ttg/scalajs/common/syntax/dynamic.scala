// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg
package scalajs
package common

import scala.scalajs.js
import js._
import js.|
import JSConverters._

import Utils.mergeJSObjects

final case class JsDynamicOps(val jsdyn: js.Dynamic) {
  @inline def asString: String        = jsdyn.asInstanceOf[String]
  @inline def asInt: Int              = jsdyn.asInstanceOf[Int]
  @inline def asArray[A]: js.Array[A] = jsdyn.asInstanceOf[js.Array[A]]
  @inline def asBoolean: Boolean      = jsdyn.asInstanceOf[Boolean]

  /** @deprecated use asJsObj */
  @inline def asJSObj: js.Object          = jsdyn.asInstanceOf[js.Object]
  @inline def asJsObj: js.Object          = jsdyn.asInstanceOf[js.Object]
  @inline def asDict[A]: js.Dictionary[A] = jsdyn.asInstanceOf[js.Dictionary[A]]
  @inline def asUndefOr[A]: js.UndefOr[A] = jsdyn.asInstanceOf[js.UndefOr[A]]
  @inline def asJsObjSub[A <: js.Object]  = jsdyn.asInstanceOf[A] // assumes its there!
  @inline def asJsArray[A <: js.Object]   = jsdyn.asInstanceOf[js.Array[A]]
  @inline def toOption[T <: js.Object]: Option[T] =
    if (js.DynamicImplicits.truthValue(jsdyn)) Some(jsdyn.asInstanceOf[T])
    else None
  @inline def toNonNullOption[T <: js.Object]: Option[T] = JsUndefOrOps(asUndefOr).toNonNullOption
  @inline def combine(that: js.Dynamic)                  = mergeJSObjects(jsdyn, that)
  @inline def toTruthy: Boolean = js.DynamicImplicits.truthValue(jsdyn)
}

trait JsDynamicSyntax {
  implicit def jsDynamicOpsSyntax(jsdyn: js.Dynamic) = JsDynamicOps(jsdyn)
}
