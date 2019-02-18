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

trait DeleteOps[
  F[_],
  PreferOptions <: BasicPreferOptions,
  RequestOptions <: BasicRequestOptions[PreferOptions]
] {
  self: ClientInfrastructure[F, PreferOptions, RequestOptions] =>

  /** Delete an entity. Return the id passed in for convenience. Return true if
    * the entity does not exist (404) even though this call did not technically
    * delete it but semantically, the entity no longer exists. HTTP returns 204
    * No Content upon success.
    * @param entitySet EntitySet
    * @param keyInfo Primbary key (GUID) or alternate key.
    * @return Tuple of id and true if deleted, false otherwise.
    */
  def delete(entitySet: EntitySetName,
             key: ODataId,
             opts: Option[RequestOptions] = None): F[(ODataId, Boolean)] = {
    // Status 204 indicates success, status 404 indicates the entity did not exist.
    val request = mkDeleteRequest(entitySet, key, opts)
    http.fetch(request) {
      case Status.Successful(resp)                               => F.pure((key, true))
      case Status.ClientError(resp) if (resp.status.code == 404) => F.pure((key, true))
      case failedResponse =>
        mkUnexpectedError(s"Delete for $entitySet($key)", request, failedResponse)
    }
  }
}

