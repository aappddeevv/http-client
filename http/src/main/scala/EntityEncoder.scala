// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg
package client
package http

import scala.scalajs.js
import js.annotation._
import fs2._
import cats._
import cats.data._
import cats.implicits._
import cats.effect._

import js.JSConverters._
import scala.annotation.implicitNotFound
import scala.collection.mutable

/** Encode a domain-specific value for HTTP request body use. This encoder is
 * not used in the base Client specifically but is part of the DSL to make
 * back-ends independent of application domain specific type. An encoder is
 * really just a co-product of HttpHeaders and a body type. The encode function
 * is a Kleisli.
 * 
 * @tparam B The type of the body effect.
 * @tparam A The application type to be sent through the client.
 * @tparam C The backend specific type to convert to.
 */
@implicitNotFound("Cannot find instance of EntityEncoder[${A}].")
trait EntityEncoder[B[_], -A, C] { self =>

  /** Alias for encode. */
  def apply(a: A) = encode(a)

  /** Encode the body. Headers could be dependent on the value, not just the
   * EntityEncoder type. The headers will generally reflect the `Content-Type`
   * e.g. "application/json" which is a string body with, say UTF-8.
   */
  def encode(a: A): (HttpHeaders, B[C])

  /** Create a new encoder from another encoder. */
  def contramap[A2](f: A2 => (HttpHeaders, A)) = new EntityEncoder[B, A2, C] {
    override def encode(b: A2): (HttpHeaders, B[C]) = {
      val (h, a) = f(b)
      val p = self.encode(a)
      // f headers take precedence
      (p._1 ++ h, p._2)
    }
  }
}

object EntityEncoder extends EntityEncoderOps {
  /** Summoner. */
  def apply[B[_], A, C](implicit encoder: EntityEncoder[B,A,C]): EntityEncoder[B,A,C] = encoder
}

trait EntityEncoderOps {
  /** Create an encoded given a function that returns just a body. Headers will be empty. */
  def encodeBy[B[_], A, C](hs: HttpHeaders)(f: A => B[C]): EntityEncoder[B,A,C] =
    new EntityEncoder[B, A, C] {
      override def encode(a: A) = (hs,f(a))
    }

  /** Create an encoder given a function that returns headers and the body. */
  def instance[B[_], A, C](f: A => (HttpHeaders, B[C])): EntityEncoder[B,A,C] = 
    new EntityEncoder[B, A, C] {
      override def encode(a: A) = f(a)
    }

  // def EmptyEncoder[F[_]: Applicative,A]: EntityEncoder[F,A] =
  //   new EntityEncoder[F,A] {
  //     def toEntity(a: A) = (HttpHeaders.empty, Entity.empty)
  //   }

  // implicit def UnitEncoder[F[_]: Applicative]: EntityEncoder[F, Unit] =
  //   EmptyEncoder[F, Unit]

  // implicit def StringEncoder[F[_]: Applicative]: EntityEncoder[F, String] =
  //   encodeBy(HttpHeaders.empty)(s => Entity(Applicative[F].pure(s)))

  // /** scalajs specific */
  // implicit def JsObjectEncoder[F[_]: Applicative, A <: js.Object]: EntityEncoder[F, A] =
  //   StringEncoder[F].contramap[A](a => js.JSON.stringify(a))

  // /** scalajs specific */
  // implicit def JsDynamicEncoder[F[_]: Applicative]: EntityEncoder[F,js.Dynamic] =
  //   StringEncoder[F].contramap[js.Dynamic](js.JSON.stringify(_))

  // /** Multitpart encoder. */
  // implicit def MultipartEntityEncoder[F[_]: Traverse: Monad]: EntityEncoder[F,Multipart[F]] =
  //   encodeBy{ m => 
  //     (
  //       HttpHeaders.empty ++ Map("Content-Type" -> Seq(Multipart.MediaType, "boundary=" + m.boundary.value)),
  //       Entity(Multipart.render(m))
  //     )
  //   }
}
