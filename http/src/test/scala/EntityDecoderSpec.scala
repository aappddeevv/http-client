// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg.odata.http

import scala.scalajs.js
import org.scalatest._
import scala.concurrent._

import cats._
import cats.data._
import cats.implicits._
import cats.effect._

import ttg.odata.client.http._

case class TestMessage(
  body: Entity[IO],
  headers: HttpHeaders = HttpHeaders.empty
) extends Message[IO]

class EntityDecoderSpec
    extends AsyncFlatSpec
    with Matchers
    with OptionValues {

  import EntityDecoder._

  def compareContent[F[_]: Functor:Semigroupal](lhs: Entity[F], rhs: Entity[F]): F[Boolean] =
    (lhs.content, rhs.content).mapN{ _ == _ }

  "EntityDecoder" should "decode a string" in {
    val m = TestMessage(Entity(IO.pure("test")))
    val dr = TextDecoder[IO].decode(m)
    dr.forall(_ == "test").map(assert(_)).unsafeToFuture
  }

  it should "decode a js.Object" in {
    val m = TestMessage(Entity(IO.pure("""{"blah":"hah"}""")))
    val dr = JsObjectDecoder[IO, TestResponse1]().decode(m)
    dr.forall(_.blah.fold(false)(_ == "hah")).map(assert(_)).unsafeToFuture
  }

  it should "decode a simple js value in a 'value' field" in {
    val m = TestMessage(Entity(IO.pure("""{"value":"hah"}""")))
    val dr = SingleValueDecoder[IO, String].decode(m)
    dr.forall(_.value.fold(false)(_ == "hah")).map(assert(_)).unsafeToFuture
  }

  // it should "decode a value array" in {
  //   val m = TestMessage(Entity(IO.pure("""{"value":["hah"]}""")))
  //   val dr = ValueArrayDecoder[IO, String].decode(m)
  //   dr.forall(_.value.fold(false)(_(0) == "hah")).map(assert(_)).unsafeToFuture    
  // }

}

trait TestResponse1 extends js.Object {
  var blah: js.UndefOr[String] = js.undefined
}
