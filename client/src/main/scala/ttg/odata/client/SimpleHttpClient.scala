// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg.odata
package client

import cats._
import http._

/**
 * Simple HTTP client that raises an `UnexpectedStatus` exception from this
 * package instead of the `UnexpectedHttpStatus` exception from the HTTP layer.
 */
object SimpleHttpClient {

  def apply[F[_]](f: HttpRequest[F] => F[HttpResponse[F]])(
    implicit M: MonadError[F,Throwable]
  ): Client[F] =
    http.Client(f, (req, resp) =>
      M.raiseError(new SimpleUnexpectedStatus(
        status = resp.status,
        request = Option(req),
        response = Option(resp)))
    )

}
