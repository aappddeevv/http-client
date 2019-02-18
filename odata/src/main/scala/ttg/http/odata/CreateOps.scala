// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg
package client
package odata

import cats.Monad
import http._
import odata.instances.odatadecoders._

trait CreateOps[F[_]] {
  self: ClientInfrastructure[F] =>

  private implicit val _F: Monad[F] = F

/** Create an entity and return the id of the object created. The id is returned
 * in the header "OData-EntityId" *if* you set the prefers to "return=minimal".
 * This function does not check the Prefer header.
  */
  def createReturnId[B](entitySet: EntitySetName, body: B, opts: Option[RequestOptions] = None)(
      implicit e: EntityEncoder[F, B]): F[String] = {
    //val newOpts = opts.copy(prefers = opts.prefers.copy(`return` = None))
    //create(entityCollection, body, newOpts)(e, ReturnedIdDecoder)
    create(entitySet, body, opts)(e, ReturnedIdDecoder)
  }

  /** Create an entity. The Location header should have a URL to pull the new
    * entity. If return=minimal, 204 No Content else 201 Created. Choose a
    * decoder to match the desired return semantics. Not setting return is
    * equivalent to "return=representation". If return=minimal the new entity id
    * is returned in the header OData-EntityId.
    */
  def create[B, R](entitySet: EntitySetName, body: B, opts: Option[RequestOptions] = None)(
      implicit enc: EntityEncoder[F, B], dec: EntityDecoder[F, R]): F[R] = {
    val request = mkCreateRequest[B](entitySet, body, opts)
    http.expectOr(request)(
      mkUnexpectedError(s"Create for $entitySet", _, _))
  }
}
