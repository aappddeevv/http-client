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
abstract class MessageFailure extends RuntimeException {
  /** Sublasses should override this to allow `getMessage` to work autuomatcally. */
  def message: String
  final override def getMessage: String = message
}

/** General decoding failure for `EntityDecoder`. */
abstract class DecodeFailure extends MessageFailure {
  def cause: Option[Throwable]     = None
  override def getCause: Throwable = cause.orNull
}

/** Exected header or header value was missing or unexpected. */
final case class MissingExpectedHeader(details: String, override val cause: Option[Throwable] = None)
    extends DecodeFailure {
  def message: String = s"Expected header: $details"
}

/** Message body was malformed in some way. */
final case class MessageBodyFailure(details: String, override val cause: Option[Throwable] = None)
    extends DecodeFailure {
  def message: String = s"Malformed body: $details"
}

/** Error to throw when something happens underneath it e.g. in the OS or
  * specific client in the implementation dependent code. The cause should be
  * the specific exception. While it is not intended this way,
  * `CommunicationsFailure` typically reroots all exceptions coming form
  * implementation dependent client code e.g. creaeting bad headers while using
  * the browser's fetch.
 */
class CommunicationsFailure(
  val details: String,
  val cause: Option[Throwable] = None
) extends MessageFailure {
  cause.foreach(initCause) // java associates a throwable with this exception
  //def message: String = s"Communications failure: $details"
  def message: String = s"$details"
}

/** A basic unexpected status object *if* you need something this simple. Since
 * the basic client has `expect` methods, we needed something that could be used
 * in the simple case.
 */
final case class UnexpectedHttpStatus(status: Status) extends RuntimeException  with scala.util.control.NoStackTrace {
  override def getMessage = s"Unexpected status: $status."
}
