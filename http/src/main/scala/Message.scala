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
  * Superclass of requests and responses. Holds headers and a body
  * at a minimum.
  */
trait Message[F[_]] extends MessageOps[F] {
  def headers: HttpHeaders
  def body: Entity[F]

  override def attemptAs[T](implicit F: FlatMap[F], decoder: EntityDecoder[F, T]): DecodeResult[F, T] =
    decoder.decode(this)
}

/**
  * Basic HTTP client code based mostly on http4s.
  */
trait MessageOps[F[_]] extends Any {

  /** Decode the body given an implicit decoder to a DecodeResult. */
  def attemptAs[T](implicit F: FlatMap[F], decoder: EntityDecoder[F, T]): DecodeResult[F, T]

  /**
    * Decode the body to specific type inside an effect. A decode exception is
    * translated into a failed effect. If the DecodeResult was already failed,
    * that failure is kept. This is just one way to handle the DecodeResult.
    */
  final def as[T](implicit F: FlatMap[F], decoder: EntityDecoder[F, T]): F[T] =
    //attemptAs(decoder).fold(F.raiseError(_), _.pure[F]).flatten
    attemptAs.fold(throw _, identity)
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

/** Request with on body, typical of a GET. */
case class HttpRequestNoBody(
  val method: Method,
  val path: String,
  val headers: HttpHeaders = HttpHeaders.empty,
  val tags: Map[String, scala.Any] = Map()
) extends HttpRequestInfo { self =>

  def body[B[_]](body: Entity[B]) = HttpRequest[B](
    method = self.method,
    path = self.path,
    headers = self.headers,
    tags = self.tags,
    body
  )
}

/** HTTP request with a body that is specified in a body effect, potentially
 * different than the effect used asynchronously in a request, response cycle.
 * It's alot easier if your asynchronous effect type matches the body's effect
 * type.
 */
case class HttpRequest[B[_]](
  val method: Method,
  val path: String,
  val headers: HttpHeaders = HttpHeaders.empty,
  val tags: Map[String, scala.Any] = Map(),
  val body: Entity[B]
)
    extends Message[B] with HttpRequestInfo { self =>

  def decodeWith[T](decoder: EntityDecoder[B, T]) =
    DecodableRequest[B, T](self, decoder)

}

object HttpRequest {
  implicit def show[F[_]]: Show[HttpRequest[F]] = Show.fromToString

  /** Convert a request to a response via "copy" with a specific status. */
  def toResponse[F[_]](request: HttpRequest[F], status: Status = Status.OK): HttpResponse[F] =
    HttpResponse(
      status = status,
      headers = request.headers,
      body = request.body,
      tags = request.tags
    )
}

/** A request that has a decoder to decode the response. */
case class DecodableRequest[B[_], T](
  request: HttpRequest[B],
  decoder: EntityDecoder[B, T]
)

/** HTTP response. */
case class HttpResponse[F[_]](
  status: Status,
  headers: HttpHeaders,
  body: Entity[F],
  tags: Map[String, scala.Any] = Map()) extends Message[F]

object HttpResponse {
  implicit def show[F[_]]: Show[HttpResponse[F]] = Show.fromToString  
}
