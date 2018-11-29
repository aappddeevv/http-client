// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg.odata
package client

import client.http.{HttpHeaders, EmptyHeaders}

/**
 * Processing for a few basic OData headers. You will need to customize these
 * for your specific back-end OData system if it has customizations.
 */
object headers {

  val FormattedValue = "OData.Community.Display.V1.FormattedValue"
  val NextLink       = "@odata.nextLink"

  /** Create an attribute name with modifier. */
  def attr(p: String, mod: String) = p + "@" + mod

  /** Basic headers. Forms a good base to add to and override. */
  val basicHeaders: HttpHeaders =
    HttpHeaders("OData-Version"    -> "4.0",
                "OData-MaxVersion" -> "4.0",
                "Cache-Control"    -> "no-cache",
                "If-None-Match"    -> "null") ++
      AcceptHeader ++
      ContentTypeJson

  val ContentTypeJson                 = HttpHeaders("Content-Type"                     -> "application/json; charset=utf-8")
  val AcceptHeader                    = HttpHeaders("Accept"                           -> "application/json")
}
