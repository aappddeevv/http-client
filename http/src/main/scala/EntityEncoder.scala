// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg.odata.client
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

/**
 * Keep it simple, entity body is ultimately a string...skip streams for the
 * body itself in this implementation. Length can be calculated directly from
 * the string.
 */
final case class Entity[+F[_]](content: F[String])

object Entity {

  implicit def entityMonoid[F[_]](implicit A: Applicative[F]): Monoid[Entity[F]] =
    new Monoid[Entity[F]] {
      def combine(l: Entity[F], r: Entity[F]): Entity[F] =
        Entity((l.content,r.content).mapN(_ + _))
      val empty: Entity[F] = Entity.empty[F]
    }

  def empty[F[_]: Applicative]: Entity[F] = Entity(Applicative[F].pure(""))
}


/** Simple encoder that encodes a value to a strict value. */
@implicitNotFound("Cannot find instance of EntityEncoder[${A}].")
trait EntityEncoder[F[_], A] { self =>

  /** Encode the body. Headers could be dependent on the value, not just the decoder "type".  */
  def toEntity(a: A): (HttpHeaders, Entity[F])

  /** Create a new encoder from another encoder. */
  def contramap[B](f: B => A): EntityEncoder[F, B] = new EntityEncoder[F, B] {
    override def toEntity(b: B): (HttpHeaders, Entity[F]) = self.toEntity(f(b))
  }

}

object EntityEncoder extends EntityEncoderInstances {
  /** Summoner. */
  def apply[F[_], A](implicit encoder: EntityEncoder[F,A]): EntityEncoder[F,A] = encoder
}

trait EntityEncoderInstances {
  /** Create an encoder. Headers not dependent on the message body. */
  def encodeBy[F[_], A](hs: HttpHeaders)(f: A => Entity[F]): EntityEncoder[F,A] =
    new EntityEncoder[F, A] {
      override def toEntity(a: A) = (hs,f(a))
    }

  /** Create an encoder. */
  def encodeBy[F[_], A](f: A => (HttpHeaders, Entity[F])): EntityEncoder[F,A] = 
    new EntityEncoder[F, A] {
      override def toEntity(a: A) = f(a)
    }

  def EmptyEncoder[F[_]: Applicative,A]: EntityEncoder[F,A] =
    new EntityEncoder[F,A] {
      def toEntity(a: A) = (HttpHeaders.empty, Entity.empty[F])
    }

  implicit def UnitEncoder[F[_]: Applicative]: EntityEncoder[F, Unit] =
    EmptyEncoder[F, Unit]

  implicit def StringEncoder[F[_]: Applicative]: EntityEncoder[F, String] =
    encodeBy(HttpHeaders.empty)(s => Entity(Applicative[F].pure(s)))

  /** scalajs specific */
  implicit def JsObjectEncoder[F[_]: Applicative, A <: js.Object]: EntityEncoder[F, A] =
    StringEncoder[F].contramap[A](a => js.JSON.stringify(a))

  /** scalajs specific */
  implicit def JsDynamicEncoder[F[_]: Applicative]: EntityEncoder[F,js.Dynamic] =
    StringEncoder[F].contramap[js.Dynamic](js.JSON.stringify(_))

  /** Multitpart encoder. */
  implicit def MultipartEntityEncoder[F[_]: Traverse: Monad]: EntityEncoder[F,Multipart[F]] =
    encodeBy{ m => 
      (
        HttpHeaders.empty ++ Map("Content-Type" -> Seq(Multipart.MediaType, "boundary=" + m.boundary.value)),
        Entity(Multipart.render(m))
      )
    }
}
