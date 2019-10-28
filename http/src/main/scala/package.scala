// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg
package client

import io.estatico.newtype.macros.newtype
import scala.scalajs.js
import collection.mutable
import concurrent.duration.FiniteDuration

import cats._
import cats.data._
import cats.effect._

/**
 * HTTP client based on the http4s design but simplified to handle only strict
 * message bodies and non-"cats Resource" based HTTP responses. The client is
 * scala.js based and not for the JVM per se.
 *
 */
package object http {

  /** 
   * Low-level middleware at the http request/response level. You can chain
   * Client's together using the `run` function (since that's a `Kleisli`).
   * {{{
   *   // Using the Middleware type with `def apply` causues implicit parameter confusion...
   *   // so we use the following signature for declaring the middleware.
   *   def coolClient[F[_]](client: Client[F])(implcit M: MonadError[F,Throwable]): Client[F] = 
   *      Client(client.run, myErrorHandler)
   * }}}
   */
  type Middleware[B1[_],C1,B2[_],C2,F[_],E] =
    Client[B1,C1,B2,C2,F,E] => Client[B1,C1,B2,C2,F,E]

  /** Basic headers are a dict of strings.
   */
  type HttpHeaders = Map[String, Seq[String]]

  /**
   * When decoding a response body, either you get an A or a DecodeFailure. The
    * effect may also carry an exception.  EitherT has a bunch of combinators
    * you can use to manipulate the results. You can retrieve values using
    * methods such as `fold` and `value`. Since all Client implementations wrap
    * the HttpResponse in an effect, the DecodeResult must also be wrapped in an
    * effect and you must use `fold` to obtain a value in an effect or the
    * effect's extract mechanism e.g. extract from EitherT using
    * `myEitherT.value` then use IO's `redeem` and then use IO's unsafeRun*
    * methods.
   * 
   * This is highly suboptimal and cats dependent but does not need to be.
    *
   * @see https://typelevel.org/cats/api/cats/data/EitherT.html
   * @see https://typelevel.org/cats/datatypes/eithert.html
   * @see https://typelevel.org/cats-effect/api/cats/effect/IO.html
    */
  type DecodeResult[F[_], A] = EitherT[F, DecodeFailure, A]

  /** This old type spec assumes F can carry an error, which may not be true and
   * we may need to detect the request type e.g. GET as part of the criteria.
   */
  // type RetryPolicy[F[_], E] =
  //   (HttpRequest[F], Either[E, HttpResponse[F]], Int) => Option[FiniteDuration]

  case class Method (asString: String) extends AnyVal
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
}
