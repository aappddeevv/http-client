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
import scala.concurrent._
import fs2._
import cats._
import cats.data._
import cats.implicits._
import cats.arrow.FunctionK
import cats.effect._
import js.Dynamic.{literal => jsobj}

// Add each individual syntax trait to this
trait AllSyntax
    extends JsDynamicSyntax
    with JsUndefOrSyntax
    with JsObjectSyntax
    with JsAnySyntax
    with FutureSyntax
    with IteratorSyntax
    with JsPromiseSyntax
    with StreamSyntax
    with OrNullSyntax
    with JSDateSyntax

// Add each individal syntax trait to this
object syntax {
  object all           extends AllSyntax
  object jsdynamic     extends JsDynamicSyntax
  object jsundefor     extends JsUndefOrSyntax
  object jsobject      extends JsObjectSyntax
  object jsany         extends JsAnySyntax
  object future        extends FutureSyntax
  object iterator      extends IteratorSyntax
  object jspromise extends JsPromiseSyntax
  object stream    extends StreamSyntax
  object ornull    extends OrNullSyntax
  object jsdates extends JSDateSyntax
}
