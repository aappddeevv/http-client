// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg.odata
package client

import scala.scalajs.js
import scala.concurrent.{Future, ExecutionContext}
import js.annotation._
import fs2._
import cats._
import cats.data._
import cats.effect._
import fs2._
import js.JSConverters._

import ttg.odata._
import http._
import client.syntax.queryspec._
import client.instances.odatadecoders._

/** An OData client that can raise an error using the request, response and a
 * message. This seems too generic actually, this does not raise an `E` so
 * it could only raise a generic error.
 */
trait ClientError[F[_]] {
  self: ClientFConstraints[F] =>

  /** Raise an error that is captured in the `F` context. */
  def mkUnexpectedError[A](
    msg: String,
    req: HttpRequest[F],
    resp: HttpResponse[F]): F[A]
}

trait HttpResources[F[_]] {
  /** HTTP client. */
  def http: client.http.Client[F]

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

trait ClientInfrastructure[F[_]]
    extends ClientError[F]
    with HttpResources[F]
    with ClientFConstraints[F]
    with ClientRequests[F]
    with ClientIdRenderer

/**
  * OData specific client. Its a thin layer over a basic HTTP client that
  * formulates the HTTP request and minimually interprets the response. This
  * class formulate the HttpRequest in an OData specific way then creates the
  * suspended action to execute the request and process the result. You'll need
  * to construct a client carefully for each specific OData backend. Since OData
  * datasources can have a number of application specific options, this trait
  * should be considered a building block that has many traits that reflect the
  * optionality. Hence, it is a pain to assemble all the pieces together.
  *
  * All of the methods either return an F or a fs2.Stream. The F or Stream must
  * be run in order to execute the operation. The client only captures the most
  * commonly used idioms of an OData web service. It's possible to have cases
  * here this client's API is insufficient for your OData url.
 * 
 * @todo: Factor out the fs2.Stream.
  *
 * @tparam F Effect for requests, must be a Monad.
  */
trait ODataClient[F[_]]
    extends ClientInfrastructure[F]
    with CollectionOps[F]
    with UpdateOps[F]
    with AssociateOps[F]
    with GetOneOps[F]
    with ActionOps[F]
    with CreateOps[F]
    with DeleteOps[F]
    with BatchOps[F] {
  self =>
}
