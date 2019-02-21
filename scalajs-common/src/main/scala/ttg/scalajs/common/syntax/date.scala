// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg
package scalajs
package common

import scala.scalajs.js

/**
  * Extension methods for js.Date.
  */
final case class JSDateOps(d: js.Date) {
  /** Add hours to a js.Date. */
  def addHours(n: Int): js.Date = new js.Date(d.getTime() + (n * 3600 * 1000))

  /** Add minutes to a js.Date. */
  def addMinutes(n: Int): js.Date = new js.Date(d.getTime() + (n * 60 * 1000))

  /** Add seconds to a js.Date. */
  def addSeconds(n: Int): js.Date =  new js.Date(d.getTime() + (n * 1000))

  /** Add millis to a js.Date. */
  def addMillis(n: Int): js.Date =  new js.Date(d.getTime() + n)
}

trait JSDateSyntax {
  implicit def jsDateOps(d: js.Date) = JSDateOps(d)
}

