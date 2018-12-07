// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg.odata
package client

import scala.scalajs.js
import js.{|, UndefOr}
import js.annotation._
import scala.annotation.unchecked.{uncheckedVariance => uv}

/**
  * Various OData 4.0 annotations that represent control information. Excludes
 * `odata.media*`. Annotations can be on a navigation property as well so this
 * structure is relevant to the toplevel annotations for convenience. Custom
 * annotations can also be defined in the form `namespace.termname` or
 * `property@namespace.termname`. These annotations are returned based on the
 * `odata.metadata` request parameter with values `none`, `minimal` or `full`.
 * If you want to create clever response processors to make pulling fields out
 * of the json response strucure easier, you may need to redefine these
 * structures without the `@js.native`.
 */
@js.native
trait AllAnnotations
    extends NavigationAnnotations
    with CollectionAnnotations {
  @JSName("@odata.context")
  var context: js.UndefOr[String] = js.native

  @JSName("@odata.type")
  var `type`: js.UndefOr[String] = js.native

  @JSName("@odata.metadataEtag")
  var metadataEtag: js.UndefOr[String] = js.native

  @JSName("@odata.editLink")
  var editLink: js.UndefOr[String] = js.native

  @JSName("@odata.readLink")
  var readLink: js.UndefOr[String] = js.native

  @JSName("@odata.id")
  var id: js.UndefOr[String|Null] = js.native

  @JSName("@odata.etag")
  var etag: js.UndefOr[String] = js.native
}

/** Annotations that *may* be returned with a collection. */
@js.native
trait CollectionAnnotations extends js.Object {
  @JSName("@odata.deltaLink")
  var deltaLink: js.UndefOr[Long] = js.native

  @JSName("@odata.nextLink")
  var nextLink: js.UndefOr[String] = js.native

  @JSName("@odata.count")
  var count: js.UndefOr[Long] = js.native
}

/** Annotations that *may* be returned on a navigation property. */
@js.native
trait NavigationAnnotations extends js.Object {
  @JSName("@odata.navigationLink")
  var navigationLink: js.UndefOr[String] = js.native

  @JSName("@odata.associationLink")
  var associationLink: js.UndefOr[String] = js.native
}

/**
  * General response envelope when a collection of entities is returned in
  * "values".. Use `A=_` if you do not care about the values. If you use $expand
  * on a collection value property it is listed under its attribute name on the
  * target entity and is *not* under the toplevel "value" fieldname.
  */
@js.native
trait ValueArrayResponse[A <: scala.Any] extends CollectionAnnotations {
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
trait ErrorResponse[+CM <: CodeMessage] extends js.Object {
  val error: js.UndefOr[CM] = js.native
}

/**
 * Spec defined. This is the minimum amount of error information available per
 * the spec. Optionally, data such as that found in `ErrorResonseDetail` may be
 * available. Its usually better to always use `ErrorResponseDetail` instead of
 * this trait.
 */
@js.native
trait CodeMessage extends js.Object {
  var code: String = js.native
  var message: String = js.native
}

/**
 * Spec defined.
 */
@js.native
trait ErrorResponseDetail[+IE <: js.Object, +CMT <: CodeMessageTarget]
    extends CodeMessage {
  var target: js.UndefOr[String] = js.native
  var details: js.UndefOr[js.Array[CMT @uv]] = js.native
  var innererror: js.UndefOr[IE @uv] = js.native  
}

@js.native
trait CodeMessageTarget extends CodeMessage {
    var target: js.UndefOr[String] = js.native
}
