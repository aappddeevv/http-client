// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg.odata.client.http

import scala.scalajs.js
import org.scalatest._

import cats._
import cats.data._
import cats.implicits._
import cats.effect._

import ttg.odata.client.http._

class ClientSpec
    extends AsyncFlatSpec
    with Matchers
    with OptionValues {

  val client = Client.identity[IO]

  def compareContent[F[_]: Functor:Semigroupal](lhs: Entity[F], rhs: Entity[F]): F[Boolean] =
    (lhs.content, rhs.content).mapN{ _ == _ }

  "Client" should "Client.identity should return the request as a response" in {
    val req = HttpRequest(
      method = Method.GET,
      path = "/entity",
      headers = HttpHeaders("blah"->"hah"),
      body = Entity(IO.pure("blah")))
    client.fetch(req)(IO.pure)
      .map(resp => assert(resp.headers.get("blah").contains(List("hah"))))
      .unsafeToFuture
  }

  it should "return just the status" in {
    val req = HttpRequest(
      method = Method.GET,
      path = "/entity",
      headers = HttpHeaders("blah"->"hah"),
      body = Entity(IO.pure("blah")))
    client.status(req)
      .map(s => assert(s == Status.OK))
      .unsafeToFuture
  }

}
