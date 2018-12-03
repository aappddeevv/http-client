// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg.odata
package client

import scala.scalajs.js
import js.annotation._
import fs2._
import cats.{Functor,Applicative,FlatMap,Monad}
import cats.data.{EitherT}
import cats.effect._
import js.JSConverters._
import scala.annotation.implicitNotFound
import scala.collection.mutable

import http.{DecodeResult,EntityDecoder}
import http.instances.entitydecoder._

/**
  * EntityDecoder instances specific to the type you want to "output" from the
  * decoding process.
  */
trait DecoderInstances {

  /** GUID regex. (scala) */
  val reg = """[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}""".r

  /**
    * A decoder that only looks at the header for an OData-EntityId
    * (case-insensitive) value and returns that, otherwise fail. To ensure that
    * the id is returned in the header, you must make sure that
    * return=representation is *not* set in the Prefer headers when the HTTP
    * call is issued.
    */
  def ReturnedIdDecoder[F[_]: Applicative]: EntityDecoder[F, String] = EntityDecoder { msg =>
    (msg.headers.get("OData-EntityId") orElse msg.headers.get("odata-entityid"))
      .map(_(0))
      .flatMap(reg.findFirstIn(_))
      .fold(DecodeResult.failure[F, String](MissingExpectedHeader("OData-EntityId")))(id => DecodeResult.success(id))
  }

  /**
    * Check for value array and if there is a value array return the first
    * element.  Otherwise cast the entire response to A directly and return
    * it. Either way, return a single value of type `T`. Because of these
    * assumptions, you could get undefined behavior. If there is more than
    * one element in the array, return an OnlyOneExpected error.
    *
    * If you are assuming a different underlying decode approach to the raw http
    * body, you need to write your own wrapper to detect the "value" array and
    * decide how to decode based on its presence. That's because its assumed in
    * this function that we will decode to a js.Object first to check for the
    * value array in the response body. This should really be called
    * `FirstElementOfValueArrayIfThereIsOneOrCastWholeMessage`.
    */
  def ValueWrapper[F[_]: Monad, A <: js.Object]: EntityDecoder[F,A] =
    JsObjectDecoder[F, ValueArrayResponse[A]].flatMapR[A] { arrresp =>
      // if no "value" array, assume its safe to cast to a single A
      arrresp.value.fold(DecodeResult.success[F, A](arrresp.asInstanceOf[A])) { arr =>
        if (arr.size > 0) DecodeResult.success(arr(0))
        else DecodeResult.failure(OnlyOneExpected(s"found ${arr.size} elements in 'value' field"))
      }
    }

  /** Use this when you expect only one value in an array or return an error.
    * @example {{{
    * val q = QuerySpec(...) // select value by name, which should be unique but is not a PK
    *  dynclient.getOne[Blah](q.url("entitysetname"))(ExpectOnlyOne)
    *    .map( ... )
    *    .recover( ... )
    * }}}
    */
  def ExpectOnlyOne[F[_]: Monad, A <: js.Object] = ValueWrapper[F, A]

  /**
    * Transform a `DecodeFailure(OnlyOneExpected)` to None if present, otherwise
    * Some. Use as an explicit encoder and use a type of `Option[A]` e.g.
   * `client.getOne[Option[A]](..)(ExpectOnlyOneToOption)`.
    */
  def ExpectOnlyOneToOption[F[_]: Monad, A <: js.Object] =
    ExpectOnlyOne[F, A].transformWith[Option[A]] {
      case Right(r)                    => DecodeResult.success(Some(r))
      case Left(OnlyOneExpected(_, _)) => DecodeResult.success(Option.empty[A])
      case Left(t) => DecodeResult.failure(t)
    }

  /**
    * Decode based on the expectation of a "value" field name that has an array
    * of "A" values. The returned value is a js Array not a scala collection.
    * If "value" fieldname is undefined, return an empty array freshly
    * allocated.
    */
  def ValueArrayDecoder[F[_]: Functor, A <: scala.Any]: EntityDecoder[F, js.Array[A]] =
    JsObjectDecoder[F, ValueArrayResponse[A]].map(_.value.getOrElse(js.Array[A]()))

  /**
    * Decode based on the expectation of a single value in a fieldname called
    * "value". You might get this when you navigate to a simple/single value
    * property on a specific entity
    * e.g. '/myentities(theguid)/somesimpleattribute'. A null value or undefined
    * value is automatically taken into account in the returned
    * Option. Preversely, you could assume that "js.Array[YourSomething]" is the
    * single value and use that instead of [[ValueArrayDecoder]].
    */
  def SingleValueDecoder[F[_]: Functor, A <: scala.Any]: EntityDecoder[F, js.UndefOr[A]] =
    JsObjectDecoder[F,SingleValueResponse[A]].map(_.value)
}
