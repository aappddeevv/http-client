// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg
package client
package http

import fs2.Stream
import cats._
import cats.data._
import cats.implicits._

/** Streaming methods that wraps a client. Streaming can arise from the http SSE
 * API or streaming protocol, perhaps even websockets. Streaming can also arise
 * from a server level paging protocol such has fetching a page with 5K objects
 * then continuing to page through the results 5K objects at a time.
 * 
 * @tparam SF Stream effect type, if the streaem takes an effect type.
 * @tparam S Stream type. If it only has 1 hole, you can make SF=Id, etc.
 */
trait ClientStreaming[B1[_], C1, B2[_], C2, F[_], E, SF[_], S[_,_]] {
  /** Fetch response and process it inside a stream. */
  def stream[A](req: HttpRequest[B1,C1]): S[SF[_], HttpResponse[B2,C2]]
 }

/**
 * A thin layer over a HTTP service that takes `HttpRequest => F[HttpResonse]`
 * and that adds implcit convenience for finding response decoders and handling
 * unsuccessful (non-200 range) responses. All responses are wrapped in an
 * effect. A higher level client may provide more nuanced error
 * handling. Implementations can constrain `E` to their algebra's root error
 * class, if desired. `EntityDecoder`s are only used in the `expect` methods if
 * the status is `Successful`(200 range). The specific Kleisli in `run` could
 * raise other errors. Keeping errors in `F` is bit of a disaster.
 * 
 * Some http client implementations would consider this trait a "backend."
 * 
 * Design is based directly on http4s design. It's really a `Kleisli. `Client`
 * is really a wrapper around a function `Request => F[Response]`.
 * 
 * It is not required, but if the response is received correctly with a status
 * that is not Ok, return a value of of type `UnexpectedHttpStatus` if your
 * error channel is built into the effect and is Throwable.
 * 
 * The types needed to fully specify a reqest/response call:
 * * encoder: B1[A] => B1[C1]
 * * remote call: B1[C1] => B2[C2]
 * * decoder: B2[C2] => DecodeResult[F, T]
 * 
 * B1, B2 can be sync/async, F needs to be async for remote calls.
 */
trait Client[B1[_], C1, B2[_], C2, F[_], +E] {

  /** Run the request. Kleisli function: HttpRequest => F[HttpResponse]. */
  def run(request: HttpRequest[B1, C1]): F[HttpResponse[B2, C2]]

  // type Helpers <: HelpersLike
  // trait HelpersLike {
  //   def request(method: Method, path: String, headers: HttpHeaders, body: B2[C2]): HttpRequest[B1,C2]
  //   def get(path: String): HttpRequest[B1,C2]
  // }
  // val helpers: Helpers

  /** Apply `f` if the underlying `run` is successful. */
  def fetch[A](request: HttpRequest[B1,C1])(f: HttpResponse[B2,C2] => F[A]): F[A]
  /** Apply `f` if underlying `run` is successful. */
  def fetch[A](request: F[HttpRequest[B1,C1]])(f: HttpResponse[B2,C2] => F[A]): F[A]

  /**
   * Fetch response, process response with a decoder `d` regardless of status,
   * Your decoder should be robust in the presence of potential error
   * information in the stats, body, or whatever could be an error.
   */
  def fetchAs[A](req: HttpRequest[B1,C1])(implicit d: EntityDecoder[B2,C2,F,A]): DecodeResult[F,A]
  /** Fetch and process with decoder regardless of status. */
  def fetchAs[A](req: F[HttpRequest[B1,C1]])(implicit d: EntityDecoder[B2,C2,F,A]): DecodeResult[F,A]

  /** Fetch and return the status. */
  def status(req: HttpRequest[B1,C1]): F[Status]
  /** Fetch and return the status. */
  def status(req: F[HttpRequest[B1,C1]]): F[Status]

  /** Submits a request and returns true if and only if the response status is
   * successful
   */
  def successful(req: HttpRequest[B1,C1]): F[Boolean]
  def successful(req: F[HttpRequest[B1,C1]]): F[Boolean]

  // def toKleisli[A](f: HttpResponse[F] => F[A]): Kleisli[F, HttpRequest[F], A]
}

/**
 * Import `client.instances.errorchannel._ to bring automatic `ErrorChannel` instances
 * based on an `ApplicativeError`.
 */
object Client {

  /** Create a new Client. Default version used. */
  def apply[B1[_], C1, B2[_], C2, F[_]: MonadError[?[_], E], E <: Throwable](
    f: HttpRequest[B1,C1] => F[HttpResponse[B2,C2]]
  ): Client[B1,C1,B2,C2,F,E] =
     new DefaultClient[B1,C1,B2,C2,F,E] {
       def run(request: HttpRequest[B1,C1]) = f(request)
     }

  /** Client that converts the request to a response directly with the status
    * indicated--Ok by default.
   */
//  def identity[F[_]: MonadError[?[_],E], E](status: Status = Status.OK): Client[F, E] =
//    Client(req => MonadError[F,E].pure(HttpRequest.toResponse(req, status)))
}
