// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg.odata
package client

import scala.scalajs.js
import js._
import cats._
import cats.effect._
import fs2._

import http._

/** OData operations for associating infomation. */
trait AssociateOps[F[_]] {
  self: ClientError[F] with HttpResources[F] with ClientFConstraints[F] with ClientRequests[F] =>

    /**
    * Associate an existing entity to another through a single or collection
    * valued navigation property.
    */
  def associate(fromEntitySet: String,
                fromEntityId: String,
                navProperty: String,
                toEntitySet: String,
                toEntityId: String,
                singleNavProperty: Boolean): F[Boolean] = {
    val request =
      mkAssociateRequest(fromEntitySet, fromEntityId, navProperty, toEntitySet, toEntityId, base, singleNavProperty)
    http.fetch(request) {
      case Status.Successful(resp) => F.pure(true)
      case failedResponse =>
        raiseError(failedResponse,
                             s"Association $fromEntitySet($fromEntityId)->$navProperty->$toEntitySet($toEntityId)",
                             Option(request))
    }
  }

  /** Disassociate an entity fram a navigation property. */
  def disassociate(fromEntitySet: String,
                   fromEntityId: String,
                   navProperty: String,
                   to: Option[String] = None): F[Boolean] = {
    val request = mkDisassocatiateRequest(fromEntitySet, fromEntityId, navProperty, to)
    http.fetch(request) {
      case Status.Successful(resp) => F.pure(true)
      case failedResponse =>
        raiseError(failedResponse,
                             s"Disassociation $fromEntitySet($fromEntityId)->$navProperty->$to",
                             Option(request))
    }
  }

}
