// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg.odata.client
package http

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

object HttpHeaders {
  val empty: HttpHeaders = collection.immutable.Map[String, Seq[String]]()

  /** Create headers from pairs of strings. Use something else to add String -> Seq[String]. */
  def apply(properties: (String, String)*): HttpHeaders = {
    val p = properties.map(d => (d._1, Seq(d._2)))
    collection.immutable.Map(p: _*)
  }

  /** Create from String -> Seq[String] pairs. */
  def from(properties: (String, Seq[String])*): HttpHeaders = properties.toMap

  /** Make a Content-ID header. */
  def contentId(id: String) = HttpHeaders("Content-ID" -> id)

  /** Render to a String. Newline is added on end if any content is rendered. */
  def render(h: HttpHeaders): String = {
    val sb = new StringBuilder()
    h.foreach {
      case (k, arr) =>
        val v = h(k).mkString(";")
        sb.append(s"$k: $v\r\n")
    }
    sb.toString()
  }
}

/** Wrapper type for a Method. */
sealed case class Method private (name: String)

object Method {
  val GET    = Method("GET")
  val POST   = Method("POST")
  val DELETE = Method("DELETE")
  val PATCH  = Method("PATCH")
  val PUT    = Method("PUT")
  val QUERY    = Method("QUERY")
  val HEAD    = Method("HEAD")
  val OPTIONS    = Method("OPTIONS")

  val all = Seq(GET, POST, DELETE, POST, PUT, QUERY, HEAD, OPTIONS)
}

trait MethodInstances {
  implicit val showForMethod: Show[Method] = Show.fromToString
}

/** HTTP request. */
case class HttpRequest[F[_]](
  method: Method,
  path: String,
  headers: HttpHeaders = HttpHeaders.empty,
  body: Entity[F],
  /** For backend use, if needed. */
  tags: Map[String, scala.Any] = Map())
    extends Message[F]

object HttpRequest {
  implicit def show[F[_]]: Show[HttpRequest[F]] = Show.fromToString

  /** Convert a request to a response with a specific status. */
  def toResponse[F[_]](request: HttpRequest[F], status: Status = Status.OK): HttpResponse[F] =
    HttpResponse(
      status = status,
      headers = request.headers,
      body = request.body,
      tags = request.tags
    )
}

/** HTTP response. */
case class HttpResponse[F[_]](
  status: Status,
  headers: HttpHeaders,
  body: Entity[F],
  tags: Map[String, scala.Any] = Map()) extends Message[F]

object HttpResponse {
  implicit def show[F[_]]: Show[HttpResponse[F]] = Show.fromToString  
}
