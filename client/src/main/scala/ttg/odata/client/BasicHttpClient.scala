// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg.odata
package client

import cats._
import http._

/**
 * Simple HTTP client Middleware that raises an `UnexpectedStatus` exception
 * from this package instead of the `UnexpectedHttpStatus` exception from the
 * HTTP layer.  Use this as the innermost middleware layer for your HTTP client.
 */
object BasicHttpClient{

  /** Create a new HTTP Client. with `BasicODataClient`s default error handler. */
  def apply[F[_]](client: http.Client[F])(
    implicit M: MonadError[F,Throwable]): http.Client[F] =
  http.Client(client.run,
    (req, resp) =>
    BasicODataClient.handleUnexpectedStatus(resp, "handling unexpected status", Option(req)))
}
