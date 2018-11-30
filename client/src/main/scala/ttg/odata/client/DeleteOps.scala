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

trait DeleteOps[F[_]] {
  self: ClientError[F]
      with HttpResources[F]
      with ClientFConstraints[F]
      with ClientRequests[F]
      with ClientIdRenderer =>

  /** Delete an entity. Return the id passed in for convenience. Return true if
    * the entity does not exist even though this call did not technically delete
    * it if it did not exist already.
    * @param entityCollection Entity
    * @param keyInfo Primary key (GUID) or alternate key.
    * @return Pair of the id passed in and true if deleted (204), false if not (404).
    */
  def delete(entitySet: String,
             key: ODataId,
             opts: Option[RequestOptions] = None): F[(ODataId, Boolean)] = {
    // Status 204 indicates success, status 404 indicates the entity did not exist.
    val request = mkDeleteRequest(entitySet, key, opts)
    http.fetch(request) {
      case Status.Successful(resp)                               => F.pure((key, true))
      case Status.ClientError(resp) if (resp.status.code == 404) => F.pure((key, true))
      case failedResponse =>
        raiseError(failedResponse, s"Delete for $entitySet($key)", Option(request))
    }
  }
}

