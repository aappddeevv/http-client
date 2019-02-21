// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg
package client
package http

import cats._
import cats.data._
import cats.implicits._
import cats.effect._

import http.instances.errorchannel._

/** Logging middleware. Assumes E = Throwable due to Async context bounds. */
object logging {

  /**
   * Log requests and responses.
   */
  def both[B1[_],C1,B2[_],C2,F[_]: Async](
    logHeader:Boolean=true,
    logBody:Boolean=true,
    log: String => F[Unit]
  )(
    client: Client[B1,C1,B2,C2,F,Throwable]
  ) = {
    requests[B1,C1,B2,C2,F,Throwable](logHeader, logBody, log)(
      responses(logHeader,logBody, log)(
        client))
  }

  /** Log requests. */
  def requests[B1[_],C1,B2[_],C2,F[_]: MonadError[?[_],E], E <: Throwable](
    logHeader:Boolean=true,
    logBody:Boolean=true,
    log: String => F[Unit])(
    client: Client[B1,C1,B2,C2,F,E]
  ) =
    Client[B1,C1,B2,C2,F,E]{req =>
      //log(req.show) *> client.run(req)
      MonadError[F,E].productR(log(req.show))(client.run(req))
    }

  /** Log responses. */
  def responses[B1[_],C1,B2[_],C2,F[_]: Async](
    logHeader:Boolean=true,
    logBody:Boolean=true,
    log: String => F[Unit])(
    client: Client[B1,C1,B2,C2,F,Throwable]
  ) = {
    val F = Async[F]
    Client[B1,C1,B2,C2,F,Throwable]{ req =>
      F.flatMap(client.run(req)){ resp =>
        //log(resp.show) *> F.delay(resp)
        F.productR(log(resp.show))(F.delay(resp))
      }
    }
  }
}
