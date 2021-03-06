// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg.scalajs
package common

import scala.scalajs.js
import js._
import js.|

/**
  * It is common in interop code to model a value as A or null but not undefined
  * even though null and undefined may both mean "absent value." See `|.merge`
  * as well.  Note that chaining many `js.|` together probably not work like you
  * think and sometimes its better to create a new target type then target
  * implicits to convert from each individual type (in the or) to the new target
  * type.
  */
final case class OrNullOps[A <: js.Any](a: A | Null) {

  /** Convert an A|Null to a well formed Option. Should we check or undefined? */
  @inline def toNonNullOption: Option[A] =
    // doesn't Option(a.asInstanceOf[A]) work?
    //if (a == null) Option.empty[A]
    //else Option(a.asInstanceOf[A])
    Option(a.asInstanceOf[A])

  /** If Null, then false, else true. */
  @inline def toTruthy: Boolean =
    if (js.DynamicImplicits.truthValue(a.asInstanceOf[js.Dynamic])) true
    else false

  /** null => undefined, otherwise A. */
  @inline def toUndefOr: js.UndefOr[A] =
    if (a == null) js.undefined
    else js.defined(a.asInstanceOf[A])

  @inline def toTruthyUndefOr: js.UndefOr[A] =
    if (js.DynamicImplicits.truthValue(a.asInstanceOf[js.Dynamic]))
      js.defined(a.asInstanceOf[A])
    else js.undefined

  /** Collapse A|Null => A but the value may be null. */
  @inline def merge: A = a.asInstanceOf[A]
}

trait OrNullSyntax {
  implicit def orNullSyntax[A <: js.Any](a: A | Null): OrNullOps[A] = OrNullOps[A](a)
}

