// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg
package scalajs
package common

import scala.scalajs.js
import js._
import js.|

import Utils._

final case class JsObjectOps(o: js.Object) {
  @inline def asDict[A]                       = o.asInstanceOf[js.Dictionary[A]]
  @inline def asAnyDict                       = o.asInstanceOf[js.Dictionary[js.Any]]
  @inline def asDyn                           = o.asInstanceOf[js.Dynamic]
  @inline def asUndefOr[A]: js.UndefOr[A]     = o.asInstanceOf[js.UndefOr[A]]
  @inline def combine(that: js.Object)        = merge(o, that)
  @inline def combine(that: js.Dictionary[_]) = merge(o, that.asInstanceOf[js.Object])
}

final case class JsDictionaryOps(o: js.Dictionary[_]) {
  @inline def asJsObj                     = o.asInstanceOf[js.Object]
  @inline def asDyn                       = o.asInstanceOf[js.Dynamic]
  @inline def asUndefOr[A]: js.UndefOr[A] = o.asInstanceOf[js.UndefOr[A]]
  @inline def combine(that: js.Dictionary[_]) =
    merge(o.asInstanceOf[js.Object], that.asInstanceOf[js.Object]).asInstanceOf[js.Dictionary[_]]
}

trait JsObjectSyntax {
  implicit def jsObjectOpsSyntax(a: js.Object)           = new JsObjectOps(a)
  implicit def jsDictonaryOpsSyntax(a: js.Dictionary[_]) = new JsDictionaryOps(a)
}
