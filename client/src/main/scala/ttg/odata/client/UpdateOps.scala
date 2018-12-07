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

/** OData operations for updating infomation. */
trait UpdateOps[F[_], E<:Throwable] {
  self: ClientError[F,E]
      with HttpResources[F,E]
      with ClientFConstraints[F,E]
      with ClientRequests[F,E] =>

    /**
    * Update a single property, return the id updated.
@see https://docs.microsoft.com/en-us/dynamics365/customer-engagement/developer/webapi/update-delete-entities-using-web-api?view=dynamics-ce-odata-9
    */
  def updateOneProperty(entitySet: String,
                        id: String,
                        property: String,
                        value: js.Any,
    opts: Option[RequestOptions]=None)(
    implicit enc: EntityEncoder[F, String]
  ): F[String] = {
    val b = js.JSON.stringify(js.Dynamic.literal("value" -> value))
    // A PUT! not POST!
    val (xtra, ent) = enc.toEntity(b)
    val request =
      HttpRequest[F](Method.PUT, s"/$entitySet($id)/$property", body = ent, headers = toHeaders(opts) ++ xtra)
    http.fetch[String](request) {
      case Status.Successful(resp) if (resp.status == Status.NoContent) => F.pure(id)
      case failedResponse                                               =>
        mkUnexpectedError(s"Update $entitySet($id)", request, failedResponse)
    }
  }

  /**
    * Update an entity. Fails if no odata-entityid is returned in the header.
    * For now, do not use return=representation.
    *
    */
  def update[B](entitySet: String,
                id: String,
                body: B,
                upsertPreventCreate: Boolean = true,
                upsertPreventUpdate: Boolean = false,
                opts: Option[RequestOptions] = None)(implicit e: EntityEncoder[F,B]): F[String] = {
    val request =
      mkUpdateRequest[B](entitySet, id, body, upsertPreventCreate, upsertPreventUpdate, opts, Some(base))
    //HttpRequest(Method.PATCH, s"/$entitySet($id)", body = Entity.fromString(body), headers = toHeaders(opts) ++ h )
    http.fetch[String](request) {
      case Status.Successful(resp) => F.pure(id)
      case failedResponse =>
        mkUnexpectedError(s"Update $entitySet($id)", request, failedResponse)
    }
  }

}
