// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg
package client
package http

import scala.collection.mutable
import cats._
import http._

/** General failure with a message member that is mapped back to `getMessage`. */
abstract class MessageFailure extends RuntimeException with scala.util.control.NoStackTrace {
  /** Sublasses should override this to allow `getMessage` to work autuomatcally. */
  def message: String
  final override def getMessage: String = message
}

/** General decoding failure for `EntityDecoder` which could reflect errors in
 * the status, header or body.
 */
abstract class DecodeFailure extends MessageFailure {
  def cause: Option[Throwable]     = None
  override def getCause: Throwable = cause.orNull
}

/** Exected header or header value was missing or unexpected. */
final case class HeaderFailure(details: String, override val cause: Option[Throwable] = None)
    extends DecodeFailure {
  def message: String = s"Header issue: $details"
}

/** Message body was malformed in some way. */
final case class MessageBodyFailure(details: String, override val cause: Option[Throwable] = None)
    extends DecodeFailure {
  def message: String = s"Body issue: $details"
}

/** A basic unexpected status object, not connected to DecodeFailure. */
final case class UnexpectedHttpStatus(status: Status, details: Option[String]=None)
//extends RuntimeException with scala.util.control.NoStackTrace {
    extends DecodeFailure {
  def message: String = s"""Unexpected status: $status: ${details.getOrElse("")}"""
}

/** Error to throw when something happens underneath it e.g. in the OS or
  * specific client in the implementation dependent code. The cause should be
  * the specific exception. While it is not intended this way,
  * `CommunicationsFailure` typically reroots all exceptions coming form
  * implementation dependent client code e.g. creating bad headers while using
  * the browser's fetch or a non-existent HTTP target (chrome's
  * ERR_CONNECTION_REFUSED). This failure lives in the effect vs the
  * DecodeFailure hiearchy indicates a malformed program--fix it! The stack is
  * kept in this exception class in case it is thrown by a sub-component
 */
class CommunicationsFailure(
  details: String,
  cause: Option[Throwable] = None
) extends MessageFailure {
  cause.foreach(initCause) // java associates a throwable with this exception
  //def message: String = s"Communications failure: $details"
  def message: String = s"$details"
}

/** This is not used in this library but is commonly needed when you need to
 * limit the amount of time a request can take.
 */
case class TooLong(details: String = "Communications took too long")
    extends CommunicationsFailure(details)
