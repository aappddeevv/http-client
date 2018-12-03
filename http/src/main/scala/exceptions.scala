// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg.odata
package client

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

/** A basic unexpected status object *if* you need something so simple. */
final case class UnexpectedHttpStatus(status: Status) extends MessageFailure {
  val message = s"Unexpected status: $status."
}

/** Error to throw when something happens underneath it e.g. in the OS. */
final case class CommunicationsFailure(details: String,
  val cause: Option[Throwable] = None) extends MessageFailure {
  cause.foreach(initCause) // java's associate a throwable with this exception
  def message: String = s"Communications failure: $details"
}

/** Message body was malformed in some way. */
final case class MessageBodyFailure(details: String, override val cause: Option[Throwable] = None)
    extends DecodeFailure {
  def message: String = s"Malformed body: $details"
}
