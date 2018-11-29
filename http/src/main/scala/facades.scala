// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg.odata.client
package http

import scala.scalajs.js
import js.UndefOr
import js.annotation._

/**
  * Use this to capture the `@odata.nextLink` link.
  */
@js.native
trait NextLinkResponse[A] extends js.Object {
  @JSName("@odata.nextLink")
  val nextLink: UndefOr[String] = js.native
}

/**
  * General response envelope when an array of values is returned in
  * "values".. Use A=_ if you do not care about the values.  This is only used
  * to process return bodies and find the "value" array that may, or may not be
  * there. You get this when querying for a list or when navigating to a
  * collection valued property. If you use $expand on a collection value
  * property it is listed under its attribute name on the target entity and is
  * *not* under the "value" fieldname.
  */
@js.native
trait ValueArrayResponse[A <: scala.Any] extends NextLinkResponse[A] {
  val value: UndefOr[js.Array[A]] = js.native
}

/**
  * The shape when using navigation properties to a single value and it is
  * returned in the field "value".
  */
@js.native
trait SingleValueResponse[A <: scala.Any] extends js.Object {
  val value: js.UndefOr[A] = js.native
}

/**
 * Toplevel trait if there is an error. A system specific error may also be
 * present. You can use `CodeMessage` or `ErrorResponseDetail` as the type
 * parameter, or roll your own.
 */
@js.native
trait ErrorResponse[CM <: CodeMessage] extends js.Object {
  val error: js.UndefOr[CM] = js.native
}

/**
 * Spec defined.
 */
@js.native
trait ErrorResponseDetail[IE <: js.Object, CMT <: CodeMessageTarget]
    extends CodeMessage {
  var target: js.UndefOr[String] = js.native
  var details: js.UndefOr[js.Array[CMT]] = js.native
  var innererror: js.UndefOr[IE] = js.native  
}

/**
 * Spec defined.
 */
@js.native
trait CodeMessage extends js.Object {
  var code: String = js.native
  var message: String = js.native
}

@js.native
trait CodeMessageTarget extends CodeMessage {
    var target: js.UndefOr[String] = js.native
}
