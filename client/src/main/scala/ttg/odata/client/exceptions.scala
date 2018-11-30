// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg.odata
package client

import http.DecodeFailure

final case class OnlyOneExpected(details: String, override val cause: Option[Throwable] = None) extends DecodeFailure {
  def message: String = s"Expected one: $details"
}
