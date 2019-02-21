// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg
package client
package http

import io.estatico.newtype.macros.newtype
import scala.scalajs.js
import scala.concurrent.ExecutionContext
import js.annotation._
import cats._
import cats.data._
import cats.implicits._
import cats.effect._
import js.JSConverters._
import scala.annotation.implicitNotFound
import scala.collection.mutable

/**
 * Superclass of http responses.
 */
trait Message[B[_], C] extends MessageOps[B, C] {
  def headers: HttpHeaders
  def body: B[C]
  def status: Status
  def tags: Map[String, scala.Any]

  /** Attempt as is here because you may need to decode a request body. */
  override def attemptAs[F[_], T](
    implicit decoder: EntityDecoder[B, C, F, T]): DecodeResult[F, T] =
    decoder.decode(this)
}

/**
  * Basic HTTP client code based mostly on http4s.
  */
trait MessageOps[B[_], C] {

  /** Decode the body given an implicit decoder to a DecodeResult. */
  def attemptAs[F[_], T](implicit decoder: EntityDecoder[B, C, F, T]): DecodeResult[F, T]

  /**
    * Extract the target value T from DecodeResult wrapped in the final F
    * effect. A decode exception is *thrown* if the decoder fails. This method
    * removes DecodeResult leaving only T or throws an error. You can skip using
    * `as` and use DecodeResult ( = EitherT) methods to extract the value out
    * more carefully. Don't use this unless you want an exception thrown for
    * error handling.
    */
  final def as[F[_]: Functor, T](implicit decoder: EntityDecoder[B, C, F, T]): F[T] =
    //attemptAs(decoder).fold(F.raiseError(_), _.pure[F]).flatten
    attemptAs.fold(throw _, identity)(Functor[F])
}

trait MethodInstances {
  implicit val showForMethod: Show[Method] = Show.fromToString
}

/** Basic HTTP request. */
sealed trait HttpRequestInfo {
  def method: Method
  def path: String
  def headers: HttpHeaders
  /* For backend use. */
  def tags: Map[String, scala.Any]
}

/** Request type with an effectful body. */
case class HttpRequest[B[_], A](
  method: Method,
  path: String,
  headers: HttpHeaders = HttpHeaders.empty,
  body: B[A],
  tags: Map[String, scala.Any] = Map()
) extends HttpRequestInfo { self =>
}

case class HttpRunnableRequest[B1[_], A, C1, B2[_], C2, F[_], T](
  method: Method,
  path: String,
  headers: HttpHeaders = HttpHeaders.empty,
  body: B1[A],  
  tags: Map[String, scala.Any] = Map(),
  encoder: EntityEncoder[B1, A, C1],
  decoder: EntityDecoder[B2, C2, F, T]
) extends HttpRequestInfo { self =>
}


object HttpRequest {
  implicit def show[B1[_],C1]: Show[HttpRequest[B1,C1]] =
    Show.fromToString
}

/** Raw HTTP response from a Client, backend specific body C is wrapped in
 * effect B. B is also Client specific and the decoders need to perform natural
 * transformation to the desired DecodeResult's effect type.
 */
case class HttpResponse[B[_], C](
  status: Status,
  headers: HttpHeaders,
  body: B[C],
  tags: Map[String, scala.Any] = Map()
) extends Message[B, C]

object HttpResponse {
  implicit def show[B2[_], C2]: Show[HttpResponse[B2,C2]] =
    Show.fromToString
}
