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

/**
 * Keep it simple, entity body is ultimately a string...skip streams for the
 * body itself in this implementation. Length can be calculated directly from
 * the string.
 */
final case class Entity[F[_]](
  /** Entity content. */
  content: F[String],
  /** Whether you can run the effect multiple times on the content. */
  isIdempotent: Boolean = true
)

object Entity {

  implicit def entityMonoid[F[_]](implicit A: Applicative[F]): Monoid[Entity[F]] =
    new Monoid[Entity[F]] {
      def combine(l: Entity[F], r: Entity[F]): Entity[F] =
        Entity((l.content,r.content).mapN(_ + _))
      val empty: Entity[F] = Entity.empty[F]
    }

  // Making this a val causes crash!
  def empty[F[_]](implicit F: Applicative[F]): Entity[F] =
    Entity(F.pure(""))
}
