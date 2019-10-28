// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg
package client
package http

import scala.scalajs.js
import js.annotation._
import fs2._
import cats.{Functor,Applicative,FlatMap,Monad}
import cats.data.{EitherT}
import cats.effect._
import js.JSConverters._
import scala.annotation.implicitNotFound
import scala.collection.mutable

/**
  * Decode a Message to a DecodeResult in the context of an effect, F, used to
  * obtain the response. After decoding you have a DecodeResult which is
  * co-product (either) an error or a value. You can fold on the decode result
  * to work with either side e.g. `mydecoderesult.fold(throw _,
  * identity)`. `EntityDecoder` is really a Kleisli: `Message => DecodeResult`
  * which is `Message[B,C] => F[Either[DecodeFailure, A]]`. Decoders are called
  * on http responses. Selection of the DecodeResult effect occurs at the call
  * site so you can fuse effects more efficiently.
 * 
 * @tparam B Effect that wraps C can be strict or non-strict.
 * @tparam C The client/backend specific "body object" returned.
 * @tparam F The output effect, wraps T.
 * @tparam T Type of the desired decoded value.
  */
@implicitNotFound("Cannot find instance of EntityDecoder[${T}].")
abstract class EntityDecoder[B[_]: Monad, C, F[_]: Monad, T] { self =>

  /** Alias for decode. */
  def apply(response: Message[B, C]): DecodeResult[F, T] = decode(response)

  /** Concrete implementations define the `decode` method. */
  def decode(response: Message[B, C]): DecodeResult[F, T]

  /** If that successful, return this results. Otherwise, return that's
   * unsuccessful result.
   */
  def ensureRight(that: EntityDecoder[B,C,F,T]) =
    new EntityDecoder[B, C, F, T] {
      override def decode(response: Message[B, C]): DecodeResult[F, T] =
        that.flatMapR(_ => self.decode((response))).decode(response)
    }

  /** Map into the value part of a DecodeResult. */
  def map[T2](f: T => T2) =
    new EntityDecoder[B, C, F, T2] {
      override def decode(response: Message[B, C]): DecodeResult[F, T2] =
        self.decode(response).map(f)
    }

  /** Flatmap into the value part of the DecodeResult. */
  def flatMapR[T2](f: T => DecodeResult[F, T2]) =
    new EntityDecoder[B, C, F, T2] {
      override def decode(response: Message[B, C]): DecodeResult[F, T2] =
        self.decode(response).flatMap(f)
    }

  def handleError(f: DecodeFailure => T)(implicit M: Functor[B]) = transform[T] {
    case Left(e)      => Right(f(e))
    case r @ Right(_) => r
  }

  def handleErrorWith(f: DecodeFailure => DecodeResult[F, T]) =
    transformWith[T] {
      case Left(e)  => f(e)
      case Right(r) => DecodeResult.success[F, T](r)
    }

  def bimap[T2](f: DecodeFailure => DecodeFailure, s: T => T2): EntityDecoder[B, C, F, T2] =
    transform[T2] {
      case Left(e)  => Left(f(e))
      case Right(r) => Right(s(r))
    }

  /**
    * Try this decoder then other if this decoder returns a decode failure. Due
    * to the process-once nature of the body, "this" decoder must really only
    * check headers or other information to allow orElse to read the body
    * correctly.
    */
  def orElse[T2 >: T](other: EntityDecoder[B, C, F, T2])  = {
    new EntityDecoder[B, C, F, T2] {
      override def decode(response: Message[B,C]): DecodeResult[F, T2] = {
        self.decode(response) orElse other.decode(response)
      }
    }
  }

  /** Covariant widenening via cast. We do it for you so you don't have to. */
  def widen[T2 >: T]: EntityDecoder[B, C, F, T2] = this.asInstanceOf[EntityDecoder[B, C, F, T2]]

  /** Transform a decode result into another decode result. */
  def transform[T2](t: Either[DecodeFailure, T] => Either[DecodeFailure, T2]) =
    new EntityDecoder[B, C, F, T2] {
      override def decode(response: Message[B, C]): DecodeResult[F, T2] =
        self.decode(response).transform(t)
    }

  def biflatMap[T2](f: DecodeFailure => DecodeResult[F, T2], s: T => DecodeResult[F, T2]) =
    transformWith[T2] {
      case Left(e)  => f(e)
      case Right(r) => s(r)
    }

  def transformWith[T2](f: Either[DecodeFailure, T] => DecodeResult[F, T2]) =
    new EntityDecoder[B, C, F, T2] {
      override def decode(response: Message[B, C]): DecodeResult[F, T2] =
        DecodeResult(
          Monad[F].flatMap(self.decode(response).value)(r => f(r).value)
        )
    }
}

/** 
 * @todo Define Alternative[EntityDecoder[F[_]]]? We already have `orElse`.
 */
object EntityDecoder {

  /** Summoner. */
  def apply[B[_], C, F[_], T](implicit ev: EntityDecoder[B, C, F, T]) = ev

  /** Lift function to create a decoder. */
  def instance[B[_]: Monad, C, F[_]: Monad, T](run: Message[B, C] => DecodeResult[F, T]) =
    new EntityDecoder[B, C, F, T] {
      def decode(response: Message[B, C]) = run(response)
    }
}

/**
  * EntityDecoder instances specific to the type you want to "output" from the
  * decoding process. Currently tied to `IO`.
  */
trait EntityDecoderInstances {
  import ttg.scalajs.common._

  // /**
  //   * Decode body to text. Since the body in a response is already text this is
  //   * the simplest decoder. We don't worry about charsets here but we should.
  //   */
  // implicit def TextDecoder[F[_]: Functor]: EntityDecoder[F, String] =
  //   EntityDecoder.instance { msg =>
  //     DecodeResult.success(msg.body.content)
  //   }

  // /* Decode to js.Object but do not use JSON.parse. Raw cast of the content. This
  //  * is really the same as TextDecoder.
  //  */
  // def rawDecoder[F[_]: Functor]: EntityDecoder[F, js.Object] =
  //   EntityDecoder.instance[F, js.Object](
  //     msg => DecodeResult.success(Functor[F].map(msg.body.content)(_.asInstanceOf[js.Object]))
  //   )

  // /**
  //   * Parsed into JSON using JSON.parse(). JSON parse could return a simple
  //   * value, not a JS object. Having said that, all response bodies (for valid
  //   * responses) from the server are objects.
  //   */
  // def jsDynamicDecoder[F[_]: Functor](
  //   reviver: Option[Reviver] = None): EntityDecoder[F, js.Dynamic] =
  //   EntityDecoder.instance[F, js.Dynamic](
  //     msg => DecodeResult.success(Functor[F].map(msg.body.content)(js.JSON.parse(_, reviver.orUndefined.orNull)))
  //   )

  // implicit def JsDynamicDecoder[F[_]: Functor]: EntityDecoder[F, js.Dynamic] =
  //   jsDynamicDecoder()

  // /** Filter on the JSON value. Create DecodeFailure if filter func returns
  //  * false.
  //  */
  // def JsonDecoderValidate[F[_]: Monad](
  //   f: js.Dynamic => Boolean,
  //   failedMsg: String = "Failed validation.",
  //   reviver: Option[Reviver] = None): EntityDecoder[F, js.Dynamic] =
  //   EntityDecoder.instance{ msg =>
  //     jsDynamicDecoder(reviver).decode(msg).flatMap { v =>
  //       if (f(v)) EitherT.rightT(v)
  //       else EitherT.leftT(MessageBodyFailure(failedMsg))
  //     }
  //   }

  // /**
  //   * Decode the body as json and cast to A instead of JSONDecoder which casts
  //   * the body to js.Dynamic. Typebounds implies that non-native scala JS traits
  //   * can use this decoder.
  //   */
  // def jsObjectDecoder[F[_]: Functor, A <: js.Object](
  //   reviver: Option[Reviver] = None): EntityDecoder[F, A] =
  //   jsDynamicDecoder(reviver).map(_.asInstanceOf[A])

  // /**
  //  * Decode a js.Object => A.  This shoulud pull in any subclass of js.Object as
  //  * well.
  //  */
  // implicit def JsObjectDecoder[F[_]: Functor, A <: js.Object]: EntityDecoder[F,A] =
  //   jsObjectDecoder()

  // /** Decode to a Blob, which in scala.js, is a subclass of js.Object. */
  // // Don't pull Blob which requires orcg.scalajs.dom.
  // //implicit def BlobObjectDecoder[F[_]: Functor]: EntityDecoder[F, Blob] =
  // //  jsObjectDecoder()

  /**
    * Ignore the response completely (status and body) and return decode "unit"
    * success. You typically use this decoder with a client type parameter of
    * `Unit` and when you only want to check that a successful status code was
    * returned or error out otherwise. Of course, you could do this at the
    * client level as well instead of the decoder.
    */
  // implicit def void[F[_]: Applicative, B[_], C]: EntityDecoder[F, B, C, Unit] =
  //   EntityDecoder.instance { _ =>
  //     DecodeResult.success(())
  //   }
}
