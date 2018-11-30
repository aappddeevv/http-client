// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg.odata
package client

import http.HttpHeaders

/**
  * Per-request parameters such as OData "prefer" headers.
  */
trait BasicRequestOptions[+PO <: BasicPreferOptions] {
  def prefers: PO
  def version: Option[String]
  def applyOptimisticConcurrency: Option[Boolean]
}
