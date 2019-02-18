// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg
package client
package odata

import http.HttpHeaders

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
