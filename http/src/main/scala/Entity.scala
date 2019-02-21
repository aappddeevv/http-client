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
 * The body of a message. The body is always wrapped in an effect even if its an
 * Entity for an outbound message from the client. While an Entity is backend
 * dependent we know that any HTTP backend needs to support content types like
 * String.
 * 
 * @tparam F The effect the content is wrapped in.
 * @tparam C The content type, application dependent.
 */
//final case class Entity[F[_]](
trait Entity[F[_], A]
//{
//  //content: EntityContent[F]
//  content: F[C]
//}

object Entity {

  // implicit def entityMonoid[F[_]](implicit A: Applicative[F]): Monoid[Entity[F]] =
  //   new Monoid[Entity[F]] {
  //     def combine(l: Entity[F], r: Entity[F]): Entity[F] =
  //       Entity((l.content, r.content).mapN(_ + _))
  //     val empty: Entity[F] = Entity.empty
  //   }

  // // Making this a val causes crash!
  // // def empty[F[_]](implicit F: Applicative[F]): Entity[F] =
  // //   Entity(F.pure(""))
  // val empty: Entity[Nothing] = Entity[Nothing](EmptyBody)
}
