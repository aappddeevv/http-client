// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg
package client
package odata

import scala.scalajs.js
import scala.concurrent.{Future, ExecutionContext}
import js.annotation._
import fs2._
import cats._
import cats.data._
import cats.effect._
import fs2._
import js.JSConverters._

import ttg.client._
import http._
import odata.syntax.queryspec._
import odata.instances.odatadecoders._

/** An OData client that can raise an exception when an unexpected status is
 * encountered. The specific way that an `F` carries an error dictates the
 * flexibility for describing errors. The exception is only created if the
 * response was received. A response may not be produced if the `F` is already
 * carrying an error.
 */
trait ClientError[F[_]] {
  /** Raise an exception that is captured in the `F` context. */
  def mkUnexpectedError[A](
    message: String,
    request: HttpRequest[F],
    response: HttpResponse[F]): F[A]
}

trait HttpResources[F[_]] {
  /** HTTP client. */
  def http: client.http.Client[F,Throwable]

  /** The base URL to potentiailly generate some of the OData requests,
   * specifically the multipart batch requests. Can also be used to set the HOST
   * HTTP header.
   */
  val base: String
}

/** Basic constraints placed on F to be mixed into the client ops parts.  These
 * just replicate some context bounds so the are only summoned once where
 * needed. These are not made implicit at this level of the client.
 */
trait ClientFConstraints[F[_]] {
  val F: MonadError[F, Throwable]
}

/** Factor out Id rendering from the call sites. */
trait ClientIdRenderer {
  import IdRenderer._

  def renderId(id: ODataId): String =
    id match {
      case x:Id => IdRenderer[Id].render(x)
      case x:AltId => IdRenderer[AltId].render(x)
    }
}

/** Collection of basic traits used in *Ops classes. */
trait ClientInfrastructure[F[_], PO <: BasicPreferOptions, RO <: BasicRequestOptions[PO]]
    extends ClientError[F]
    with ClientRequests[F, PO, RO]
    with HttpResources[F]
    with ClientFConstraints[F]
    with ClientIdRenderer

/**
  * OData specific client. Its a thin layer over a basic HTTP client that
  * formulates the HTTP request and minimually interprets the response. This
  * class formulate the HttpRequest in an OData specific way then creates the
  * suspended action. You still need to execute the request and process the
  * result. You'll need to construct a client carefully for each specific OData
  * backend. Since OData datasources can have a number of application specific
  * options, this trait should be considered a building block built on many
  * traits that reflect the optionality. Hence, it is a pain to assemble all the
  * pieces together. The type parameters represent the different aspects of a
  * OData client that tyically need to be overriden e.g. a specific OData server
  * can implement different Prefer headers that you may wish to provide typesafe
  * support for.
  *
  * All of the methods either return an F or a fs2.Stream. The F or Stream must
  * be run in order to execute the operation. The client only captures the most
  * commonly used idioms of an OData web service. It's possible to have cases
  * here this client's API is insufficient for your OData url.
 * 
 * @todo: Factor out the fs2.Stream.
  *
 * @tparam F Effect for requests.
  */
trait ODataClient[F[_], PO <: BasicPreferOptions, RO <: BasicRequestOptions[PO]]
    extends ClientInfrastructure[F, PO, RO]
    with CollectionOps[F, PO, RO]
    with UpdateOps[F, PO, RO]
    with AssociateOps[F, PO, RO]
    with GetOneOps[F, PO, RO]
    with ActionOps[F, PO, RO]
    with CreateOps[F, PO, RO]
    with DeleteOps[F, PO, RO]
    with BatchOps[F, PO, RO] {
  self =>
}
