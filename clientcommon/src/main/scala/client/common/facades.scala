// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg.odata.client
package http

import scala.scalajs.js
import js.UndefOr

/** Basic connection information */
trait ConnectionInfo extends js.Object {
  var tenant: UndefOr[String]               = js.undefined
  var username: UndefOr[String]             = js.undefined
  var password: UndefOr[String]             = js.undefined
  var applicationId: UndefOr[String]        = js.undefined
  var dataUrl: UndefOr[String]              = js.undefined
  var acquireTokenResource: UndefOr[String] = js.undefined
  var authorityHostUrl: UndefOr[String]     = js.undefined
  var renewalFraction: UndefOr[Int]         = js.undefined
  var secret: UndefOr[String]         = js.undefined
}
