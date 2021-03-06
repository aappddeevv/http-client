// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg
package client
package msad

import scala.scalajs.js
import js._
import annotation._
import dynamics.common._

/**
  *  From js ADAL library. For details on using ADAL in general: https://msdn.microsoft.com/en-us/library/gg327838.aspx.
  */
@js.native
@JSImport("adal-node", "AuthenticationContext")
class AuthenticationContext(authority: String, validateAuthority: Boolean = true) extends js.Object {
  def authority: js.Object          = js.native
  def options: js.Object            = js.native
  def options_=(o: js.Object): Unit = js.native
  def cache: js.Object              = js.native

  /** Use client credentials. */
  def acquireTokenWithClientCredentials(resource: String,
    applicationId: String,
    secret: String,
    callback: js.Function2[js.Error, // can be null but not undefined
      UndefOr[ErrorResponse | TokenInfo],
      Unit]): Unit = js.native

  /** Use username/password combination. */
  def acquireTokenWithUsernamePassword(resource: String,
                                       username: String,
                                       password: String,
                                       applicationId: String,
                                       callback: js.Function2[js.Error, // can be null but not undefined
                                                              UndefOr[ErrorResponse | TokenInfo],
                                         Unit]): Unit = js.native

  /** Use username/password combination. */
  def acquireTokenWithAuthorizationCode(authorizationCode: String,
                                       redirectUri: String,
                                       resource: String,
    applicationId: String,
    secret: String,
                                       callback: js.Function2[js.Error, // can be null but not undefined
                                                              UndefOr[ErrorResponse | TokenInfo],
                                         Unit]): Unit = js.native

    /** Use username/password combination. */
  def acquireTokenWithRefreshtoken(refreshToken: String,
    applicationId: String,
    ecret: String,
    resource: String,
                                           callback: js.Function2[js.Error, // can be null but not undefined
                                                              UndefOr[ErrorResponse | TokenInfo],
                                         Unit]): Unit = js.native


}
