// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg.odata
package client

import http.HttpHeaders

/**
  * Per-request parameters such as OData "prefer" headers.
  */
trait RequestOptions[O <: BasicPreferOptions] {
  def prefers: O
  def version: Option[String]
  def applyOptimisticConcurrency: Option[Boolean]
}

/** 
 * Renderer of an object destined to be turned into an `HttpHeader` to be
 * integrated into a `HttpRequest`. Some prefer options must be rendered
 * together because combinations can be collapsed to a simpler rendered output
 * e.g. `odata.include-annotations`.
 */
trait HeaderRenderer[A] {
  /** Render to a `HttpHeaders`. */
  def render(a: A): HttpHeaders
}

object HeaderRenderer {
  def apply[A](implicit hr: HeaderRenderer[A]) = hr

  def instance[A](f: A => HttpHeaders) =
    new HeaderRenderer[A] {
      def render(a: A) = f(a)
    }
}
