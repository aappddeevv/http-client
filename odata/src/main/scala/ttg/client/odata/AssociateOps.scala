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
import http.instances.entityencoder._

/** OData operations for associating/dissociating infomation. */
trait AssociateOps[
  F[_],
  PreferOptions <: BasicPreferOptions,
  RequestOptions <: BasicRequestOptions[PreferOptions]
] {
  self: ClientInfrastructure[F, PreferOptions, RequestOptions] =>

  private implicit val _F: Monad[F] = F

    /**
    * Associate an existing entity to another through a single or collection
    * valued navigation property.
    */
  def associate(fromEntitySet: EntitySetName,
                fromEntityId: ODataId,
                navProperty: String,
                toEntitySet: EntitySetName,
                toEntityId: ODataId,
                singleNavProperty: Boolean): F[Boolean] = {
    val request =
      mkAssociateRequest(
        fromEntitySet, fromEntityId, navProperty,
        toEntitySet, toEntityId, base, singleNavProperty
      )
    http.fetch(request) {
      case Status.Successful(resp) => F.pure(true)
      case failedResponse =>
        mkUnexpectedError(s"Association ${fromEntitySet.asString}($fromEntityId)->$navProperty->${toEntitySet.asString}($toEntityId)",
          request, failedResponse)
    }
  }

  /** Disassociate an entity fram a navigation property. */
  def disassociate(fromEntitySet: EntitySetName,
                   fromEntityId: ODataId,
                   navProperty: String,
                   to: Option[String] = None): F[Boolean] = {
    val request = mkDisassocatiateRequest(fromEntitySet, fromEntityId, navProperty, to)
    http.fetch(request) {
      case Status.Successful(resp) => F.pure(true)
      case failedResponse =>
        mkUnexpectedError(s"Disassociation ${fromEntitySet.asString}($fromEntityId)->$navProperty->$to",
          request, failedResponse)
    }
  }

}
