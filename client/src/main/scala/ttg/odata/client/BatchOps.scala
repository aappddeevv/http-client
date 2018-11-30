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

trait BatchOps[F[_]] {
  self: ClientError[F] with HttpResources[F] with ClientFConstraints[F] with ClientRequests[F] =>

  /**
    * Run a batch request. Dynamics batch requests are POSTs.
    * @param headers Headers for the post. *not* for each request.
    * @param opts dynamics options such as "prefer"
    */
  def batch[R](m: Multipart[F], headers: HttpHeaders = HttpHeaders.empty, opts: Option[RequestOptions] = None)(
      implicit enc: EntityEncoder[F, Multipart[F]], dec: EntityDecoder[F, R]): F[R] = {
    val (xtras, ent) = enc.toEntity(m)
    val therequest =
      HttpRequest[F](Method.POST, "/$batch", headers = headers ++ toHeaders(opts) ++ xtras, body = ent)
    http.fetch[R](therequest) {
      case Status.Successful(resp) => resp.as[R]
      case failedResponse =>
        raiseError(failedResponse, s"Batch", Option(therequest))
    }
  }
}
