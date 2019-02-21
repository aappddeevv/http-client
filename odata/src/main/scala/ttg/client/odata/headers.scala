// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg
package client
package odata

import client.http.HttpHeaders

/**
 * Processing for a few basic OData headers. You will need to customize these
 * for your specific back-end OData system if it has customizations.
 */
object headers {

  /**
   * https://github.com/OData/vocabularies/blob/master/OData.Community.Display.V1.md. Should
   * this render as "Displaya.Formatted"?
   */
  val FormattedValue = "OData.Community.Display.V1.FormattedValue"
  //val NextLink       = "@odata.nextLink"

  /** Create an attribute name with modifier. */
  def attr(p: String, mod: String) = p + "@" + mod

  val ContentTypeJson                 = HttpHeaders("Content-Type"                     -> "application/json; charset=utf-8")
  val AcceptHeader                    = HttpHeaders("Accept"                           -> "application/json")

  /** Basic headers. Forms a good base to add to and override. */
  val basicHeaders: HttpHeaders =
    HttpHeaders(
      "OData-Version"    -> "4.0",
      "OData-MaxVersion" -> "4.0",
      //"Cache-Control"    -> "no-cache",
      //"If-None-Match"    -> "null"
    ) ++ AcceptHeader ++ ContentTypeJson
}

/** 
 * Renderer of an object destined to be turned into an `HttpHeader` to be
 * integrated into a `HttpRequest`.
 */
trait HeaderRenderer[A] {
  /** Render to a `HttpHeaders`. */
  def apply(a: A): HttpHeaders
}

object HeaderRenderer extends HeaderRendererInstances {
  def apply[A](implicit hr: HeaderRenderer[A]) = hr

  def instance[A](f: A => HttpHeaders) =
    new HeaderRenderer[A] {
      def apply(a: A) = f(a)
    }
}

/** These are not added to _.instances because they operate at the "client"
 * level vs formulating requests.
 */
trait HeaderRendererInstances {
  /**
   * Some prefer options must be rendered together because combinations can be
   * collapsed to a simpler rendered output e.g. `odata.include-annotations`.
   */
  implicit def prefersHeaderRenderer[PO <: BasicPreferOptions]: HeaderRenderer[PO] =
    HeaderRenderer.instance { p =>
      prefer.render(p).fold(HttpHeaders.empty)(str => HttpHeaders("Prefer" -> str))
    }
}
