// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg
package client
package http

import fs2._
import cats._
import cats.data._
import cats.implicits._
import cats.effect._

final case class DecodeResultOps[F[_], T](val dr: DecodeResult[F, T])(
  implicit F: MonadError[F, Throwable]) {

  /** Flatten `EitherT[F,DecodeFailure,T]`. Push any DecodeFailure as the error
   * into a `F[T]` discarding any existing error that F may hold.
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

final case class RequestNoBodyOps(r: HttpRequestNoBody) extends AnyVal {
  // empty for now!
}

final case class RequestOps[B[_]](r: HttpRequest[B]) extends AnyVal {
  // empty for now!
}

final case class DecodableRequestOps[B[_], T](dr: DecodableRequest[B,T]) {
  //def send[F[_]](implicit client: Client[F[_], E]): F[T] = client.fetchAs(dr.decoder)
}

trait RequestSyntax {
  implicit def requestNoBodyOps(r: HttpRequestNoBody) = RequestNoBodyOps(r)
  implicit def requestOps[B[_]](r: HttpRequest[B]) = RequestOps[B](r)
  implicit def decodableRequestOps[B[_], T](r: DecodableRequest[B, T]) =
    DecodableRequestOps[B, T](r)
}

trait AllSyntax
    extends DecodeResultSyntax
    with MultipartSyntax
    with ErrorChannelOpsSyntax
    with RequestSyntax

object syntax {
  object all           extends AllSyntax
  object decoderesult  extends DecodeResultSyntax
  object multipart extends MultipartSyntax
  object errorchannel extends ErrorChannelOpsSyntax
  object request extends RequestSyntax
}

trait AllInstances
    extends EntityEncoderInstances
    with EntityDecoderInstances
    with MethodInstances
    with ErrorChannelInstances

object instances {
  object all           extends AllInstances
  object entityencoder extends EntityEncoderInstances
  object entitydecoder extends EntityDecoderInstances
  object method        extends MethodInstances
  object errorchannel extends ErrorChannelInstances
}

object implicits extends AllSyntax with AllInstances
