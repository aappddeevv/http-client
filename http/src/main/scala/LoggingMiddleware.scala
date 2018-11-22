// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg.odata.client
package http

import cats._
import cats.data._
import cats.implicits._
import cats.effect._

/**
 * Middleware that logs requests and responses.
 */
object LoggingMiddleware {
  /** Create a new Client. */
  def apply[F[_]: Async](
    logHeader:Boolean=true,
    logBody:Boolean=true,
    log: String => F[Unit]): Middleware[F] =
    client => 
  RequestLoggingMiddleware(logHeader, logBody, log)(Async[F])(
    ResponseLoggingMiddleware.apply(logHeader,logBody, log)(Async[F])(
      client))

}

/** Log requests. */
object RequestLoggingMiddleware {
  def apply[F[_]](
    logHeader:Boolean=true,
    logBody:Boolean=true,
    log: String => F[Unit])(
    implicit F: Async[F]): Middleware[F] =
    client => Client{ req =>
      log(req.show) *> client.run(req)
    }
}

/** Log requests. */
object ResponseLoggingMiddleware {
  def apply[F[_]](
    logHeader:Boolean=true,
    logBody:Boolean=true,
    log: String => F[Unit])(
    implicit F: Async[F]): Middleware[F] =
    client => Client{ req =>
      client.run(req).flatMap{ resp =>
        log(resp.show) *> F.delay(resp)
      }
    }
}
