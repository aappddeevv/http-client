// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg.odata.client.http

import scala.scalajs.js
import org.scalatest._
import scala.concurrent._

import cats._
import cats.data._
import cats.implicits._
import cats.effect._

import ttg.odata.client.http._

class EntityEncoderSpec
    extends AsyncFlatSpec
    with Matchers
    with OptionValues {

  import EntityEncoder._

  def compareContent[F[_]: Functor:Semigroupal](lhs: Entity[F], rhs: Entity[F]): F[Boolean] =
    (lhs.content, rhs.content).mapN{ _ == _ }

  "EntityEncoder" should "allow equal Entities" in {
    val e1 = Entity(IO.pure("blah"))
    val e2 = Entity(IO.pure("blah"))
    compareContent(e1,e2).map(assert(_)).unsafeToFuture
  }

  it should "detect unequal Entities" in {
    val e1 = Entity(IO.pure("blah"))
    val e2 = Entity(IO.pure("nah"))
    compareContent(e1,e2).map(v => assert(!v)).unsafeToFuture
  }

  it should "have an empty entity" in {
    Entity.empty[IO].content.unsafeToFuture.map(v => assert(v == ""))
  }

  it should "encode a simple string" in {
    val (hdr, ent) = stringEncoder[IO].toEntity("blah")
    compareContent(ent,Entity(IO.pure("blah")))
    .unsafeToFuture
    .map{ c => 
      assert(hdr.size == 0 && c)
    }
  }

  it should "encode a js.Object" in {
    val test1 = new Test1 { blah = "hah" }
    val (hdr, ent) = jsObjectEncoder[IO, Test1].toEntity(test1)
    compareContent(ent,Entity(IO.pure("""{"blah":"hah"}""")))
      .unsafeToFuture
      .map(v => assert(v && hdr.size == 0))
  }

  it should "encode a dynamics object" in {
    val (hdr, ent) = jsDynamicEncoder[IO].toEntity(js.Dynamic.literal("blah" -> "hah"))
    compareContent(ent,Entity(IO.pure("""{"blah":"hah"}""")))
      .unsafeToFuture
      .map(v => assert(v && hdr.size == 0))
  }
}

trait Test1 extends js.Object {
  var blah: js.UndefOr[String] = js.undefined
}
