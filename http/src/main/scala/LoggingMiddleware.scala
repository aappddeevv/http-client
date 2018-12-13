// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg.odata.client
package http

import cats._
import cats.data._
import cats.implicits._
import cats.effect._

import http.instances.errorchannel._

/** Logging middleware. */
object logging {

  /**
   * Log requests and responses.
   */
  def both[F[_]: Async](
    logHeader:Boolean=true,
    logBody:Boolean=true,
    log: String => F[Unit]
  )(
    client: Client[F]
  ): Client[F] =
    requests(logHeader, logBody, log)(
      responses(logHeader,logBody, log)(
        client))

  /** Log requests. */
  def requests[F[_]: Async](
    logHeader:Boolean=true,
    logBody:Boolean=true,
    log: String => F[Unit])(client: Client[F]): Client[F] =
    Client{req =>
      //log(req.show) *> client.run(req)
      Async[F].productR(log(req.show))(client.run(req))
    }

  /** Log responses. */
  def responses[F[_]: Async](
    logHeader:Boolean=true,
    logBody:Boolean=true,
    log: String => F[Unit])(client: Client[F]): Client[F] =
    Client{ req =>
      client.run(req).flatMap{ resp =>
        //log(resp.show) *> F.delay(resp)
        Async[F].productR(log(resp.show))(Async[F].delay(resp))
      }
    }
}
