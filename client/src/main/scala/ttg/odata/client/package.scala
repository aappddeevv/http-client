// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg.odata
import io.estatico.newtype.macros.newtype

import scala.scalajs.js
import js.|
import dynamics.common.Utils.{merge}
import ttg.scalajs.common.JSDataHelpers


package object client {
  ///** Id newtype. */
  //@newtype case class Id(asString: String)

  /** Entity set name newtype. */
  @newtype case class EntitySetName(asString: String)

  /** Entity logical name newtype. */
  @newtype case class EntityLogicalName(asString: String)

  /** Any type of logical name. */
  @newtype case class LogicalName(asString: String)

  val defaultMappers = Map[String, String => String](
    headers.FormattedValue               -> (_ + "_fv")
  )

  val defaultODataToOmit = Seq[String](
    headers.FormattedValue
  )

  /** All 0s GUID */
  val zeroGUID = "00000000-0000-0000-0000-000000000000"

    /**
    * Remap OData response artifacts if found. Note that if O is a
    * non-native JS trait, you may want to ensure that you "add" attributes to
    * your trait that match the conventions specified so you can access the
    * attributes directly later. Mapping function is (attribute name) => (mapped
    * attribute name). key for mappers are the odata extensions. Object is
    * mutated directly. Clone prior if you want a copy. Restructuring these
    * attributes is fairly expensive so it should not be done by default.
    */
  def remapODataFields[O <: js.Object](obj: O, mappers: Map[String, String => String] = defaultMappers): O = {
    val d            = obj.asInstanceOf[js.Dictionary[js.Any]]
    val mergemeafter = js.Dictionary.empty[js.Any]
    for ((key, value) <- d) {
      // test each mapper! very expensive
      for ((mkey, f) <- mappers) {
        val odataKey = s"$key@$mkey"

        if (d.contains(odataKey))
          mergemeafter(f(key)) = d(odataKey)
      }
    }
    merge[O](obj, mergemeafter.asInstanceOf[O])
  }

  /** Remove any attribute that has a `@` annotation in its name. */
  def dropODataFields[O <: js.Object](obj: O, patterns: Seq[String] = defaultODataToOmit): O =
    JSDataHelpers.omitIfMatch(obj.asInstanceOf[js.Dictionary[js.Any]], patterns).asInstanceOf[O]

  /** Cast to CodeMessage if "code" is defined on the object. */
  def maybeError[CM <: CodeMessage]: PartialFunction[js.Object, CM] =
  {
    case err@_ if(
      js.Object.hasOwnProperty("error") &&
        js.Object.hasOwnProperty("message")) => err.asInstanceOf[CM]
  }
}
