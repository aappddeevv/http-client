// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg.odata.client
package http

import fs2._
import cats._
import cats.data._
import cats.implicits._
import cats.effect._

final case class DecodeResultOps[F[_], T](val dr: DecodeResult[F, T])(
  implicit F: MonadError[F, Throwable]) {

  /** Flatten `EitherT[F,DecodeFailure,T]`. Push any DecodeFailure as the error
   * into a `F[T]`.
   */
  def toF: F[T] = F.flatten(dr.fold(F.raiseError, F.pure))
}

trait DecodeResultSyntax {
  implicit def decodeResultOpsSyntax[F[_], T](dr: DecodeResult[F, T])(implicit F: MonadError[F, Throwable]) =
    DecodeResultOps(dr)
}

final case class MultipartOps[F[_]](r: HttpRequest[F]) {
  def toPart = SinglePart(r)
}

trait MultipartSyntax {
  implicit def multiartOpsSyntax[F[_], T](r: HttpRequest[F]) = MultipartOps(r)
}

trait AllSyntax
    extends DecodeResultSyntax
    with MultipartSyntax

object syntax {
  object all           extends AllSyntax
  object decoderesult  extends DecodeResultSyntax
  object multipart extends MultipartSyntax
}

trait AllInstances
    extends EntityEncoderInstances
    with EntityDecoderInstances
    with MethodInstances

object instances {
  object all           extends AllInstances
  object entityencoder extends EntityEncoderInstances
  object entitydecoder extends EntityDecoderInstances
  object method        extends MethodInstances
}

object implicits extends AllSyntax with AllInstances
