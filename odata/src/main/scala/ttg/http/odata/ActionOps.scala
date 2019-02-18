// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg
package client
package odata

import scala.scalajs.js
import js._
import cats._
import cats.effect._
import fs2._

import http._

trait ActionOps[F[_]] {
  self: ClientInfrastructure[F] =>

  /**
    * Execute an action. Can be bound or unbound depending on entityCollectionAndId. Action
    * name may be prefixed with a namespace like `Microsoft.Dynamics.CRM`.
    *
    * TODO: Map into a http.expect.
    */
  def executeAction[A, B](action: String,
                       body: A,
                       entitySetAndId: Option[(String, String)] = None,
    opts: Option[RequestOptions]=None)(
    implicit E: EntityEncoder[F, A], D: EntityDecoder[F, B]): F[B] = {
    val request = mkExecuteActionRequest(action, body, entitySetAndId, opts)
    http.expectOr(request)(
      mkUnexpectedError(s"Executing action $action, $body, $entitySetAndId",_, _))
  }

  /** Exceute a bound or unbound function.
    *
    * @param function Function name
    * @param parameters Query parameters for function.
    * @param entity Optional (entityCollection, id) info for bound function call. Use None for unbound function call.
    * @param d Decode response body.
    * @return
    */
  def executeFunction[A](function: String,
                         parameters: Map[String, scala.Any] = Map.empty,
                         entity: Option[(String, String)] = None)(implicit d: EntityDecoder[F, A]): F[A] = {
    val request = mkExecuteFunctionRequest(function, parameters, entity)
    http.expectOr(request)(
      mkUnexpectedError(s"Execute function: $function", _, _))
  }
  
}
