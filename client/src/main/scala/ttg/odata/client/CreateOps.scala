// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg.odata
package client

import http._

trait CreateOps[F[_]] {
  self: ClientError[F] with HttpResources[F] with ClientFConstraints[F] with ClientRequests[F] =>

// /** Create an entity and expect only its id returned. includeRepresentation is
//   * set to an explicit false in the headers to ensure the id is returend in the
//   * header. Does this work with all OData clients?
//   */
  // def createReturnId[B](entityCollection: String, body: B, opts: Option[RequestOptions] = None)(
  //     implicit e: EntityEncoder[F, B]): F[String] = {
  //   val newOpts = opts.copy(prefers = opts.prefers.copy(`return` = None))
  //   create(entityCollection, body, newOpts)(e, ReturnedIdDecoder)
  // }

  /** Create an entity. The Location header should have a URL to pull the new
    * entity. If return=minimal, 204 No Content else 201 Created. Choose a
    * decoder to match the desired return semantics.
    */
  def create[B, R](entitySet: String, body: B, opts: Option[RequestOptions] = None)(
      implicit enc: EntityEncoder[F, B], dec: EntityDecoder[F, R]): F[R] = {
    val request = mkCreateRequest[B](entitySet, body, opts)
    http.fetch(request) {
      case Status.Successful(resp) => resp.as[R]
      case failedResponse =>
        raiseError(failedResponse, s"Create for $entitySet", Option(request))
    }
  }
}
