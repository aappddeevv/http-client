// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg.odata
package client

import scala.scalajs.js
import scala.collection.mutable
import cats._
import client.http._

trait HTTPExceptionsInstances {
  implicit def showForUnexpectedStatus[F[_]: Monad] = Show.show[SimpleUnexpectedStatus[F]]{ ex =>
    val reason = Option(ex.status.reason).map("(" + _ + ")").getOrElse("")
    s"UnexpectedStatus status=${ex.status}$reason"
  }
}

/** Exception with a possible OData spec defined error data structure, if
 * present.  The OData protocol carries a potential error.code field present on
 * the "error" field member. It acts as a "sub-code" to the HTTP response. The
 * idea is to expose the error code for pattern matching under. Its is kept is
 * an Option since the error payload is optional. In a sense, the odata code message when there is an error, is alot like the "cause" property on java's `RuntimeException`.
 * 
 */
abstract class ODataException[F[_], +CM <: CodeMessage]() extends MessageFailure {
 /** An OData error that may be present. */
  def odata: Option[CM] = None
}

/**
  * Unexpected status returned, the original request and response may be
  * available. This is a very common error when working with a REST service.
  * The `note` indicates something specific about where the exception was
  * generated.
  */
class UnexpectedStatus[F[_], +CM <: CodeMessage](
  status: Status,
  odata: Option[CM] = None,
  request: Option[HttpRequest[F]] = None,
  response: Option[HttpResponse[F]] = None,
  note: Option[String] = None)
    extends ODataException[F, CM] {
  // keep this? could leave toString!?!?!
  val message: String = s"""Unexpected HTTP status: $status: ${note.getOrElse("")}"""
  override def toString(): String = {
    s"""UnexpectedStatus: status=$status${Option(status.reason).map("(" + _ + ")").getOrElse("")}, request=${request
      .toString()}, response=${response.toString()}, note=${note.getOrElse("")}"""
  }
}

/** UnexpectedStatus with the inner error modelled as a `js.Object`. */
final case class SimpleUnexpectedStatus[F[_]](
  status: Status,
  request: Option[HttpRequest[F]] = None,
  response: Option[HttpResponse[F]] = None,
  odataError: Option[ErrorResponseDetail[js.Object, CodeMessageTarget]] = None,
  note: Option[String] = None)
    extends UnexpectedStatus[F, ODataErrorType[js.Object]](status=status,
      odata=odataError, request=request, response=response, note=note) {
}

final case class OnlyOneExpected(details: String,
  override val cause: Option[Throwable] = None) extends DecodeFailure {
  def message: String = s"Expected one: $details"
}
