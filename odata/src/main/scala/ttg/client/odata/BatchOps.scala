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

trait BatchOps[
  F[_],
  PreferOptions <: BasicPreferOptions,
  RequestOptions <: BasicRequestOptions[PreferOptions]
] {
  self: ClientInfrastructure[F, PreferOptions, RequestOptions] =>

  /**
    * Run a batch request. Dynamics batch requests are POSTs.
    * @param headers Headers for the post. *not* for each request.
    * @param opts dynamics options such as "prefer"
    */
  def batch[R](m: Multipart[F], headers: HttpHeaders = HttpHeaders.empty, opts: Option[RequestOptions] = None)(
      implicit enc: EntityEncoder[F, Multipart[F]], dec: EntityDecoder[F, R]): F[R] = {
    val (xtras, ent) = enc.toEntity(m)
    val request =
      HttpRequest[F](Method.POST, "/$batch", headers = headers ++ toHeaders(opts) ++ xtras, body = ent)
    http.expectOr(request)(
      mkUnexpectedError("Batch", _, _))
  }
}
