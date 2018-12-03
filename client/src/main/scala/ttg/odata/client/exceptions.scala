// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg.odata
package client

import scala.collection.mutable
import cats._
import client.http._

trait HTTPExceptionsInstances {
  implicit def showForUnexpectedStatus[F[_]: Monad] = Show.show[SimpleUnexpectedStatus[F]]{ ex =>
    val reason = Option(ex.status.reason).map("(" + _ + ")").getOrElse("")
    s"UnexpectedStatus status=${ex.status}$reason"
  }
}

/** Exception with a possible OData specific error data structure. */
abstract class ODataException[F[_], CM <: CodeMessage]() extends MessageFailure {
 /** An OData error that may be present. */
  def odata: Option[CM] = None
}

/**
  * Unexpected status returned, the original request and response may be
  * available. This is a very common error when working with a REST service.
  * The `note` indicates something specific about where the exception was
  * generated.
  */
class UnexpectedStatus[F[_], CM <: CodeMessage](status: Status,
  odata: Option[CM] = None,
  request: Option[HttpRequest[F]] = None,
  response: Option[HttpResponse[F]] = None,
  note: Option[String] = None)
    extends ODataException {
  // keep this? could leave toString!?!?!
  val message: String = s"Unexpected HTTP status: $status"
  override def toString(): String = {
    s"""UnexpectedStatus: status=$status${Option(status.reason).map("(" + _ + ")").getOrElse("")}, request=${request
      .toString()}, response=${response.toString()}, note=${note.getOrElse("")}"""
  }
}

final case class SimpleUnexpectedStatus[F[_]](
  status: Status,
  request: Option[HttpRequest[F]] = None,
  response: Option[HttpResponse[F]] = None,
  odataError: Option[CodeMessage] = None)
    extends UnexpectedStatus[F,CodeMessage](status=status,
      odata=odataError, request=request, response=response) {
}

final case class OnlyOneExpected(details: String,
  override val cause: Option[Throwable] = None) extends DecodeFailure {
  def message: String = s"Expected one: $details"
}
