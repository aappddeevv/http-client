// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg.odata
package client
package browserfetch

import scala.scalajs.js
import org.scalatest._
import scala.concurrent._
import org.scalajs.dom.experimental._

import cats._
import cats.data._
import cats.implicits._
import cats.effect._

import client.http._
import BrowserFetch._

class BrowserFetchSpec
    extends FlatSpec
    with Matchers
    with OptionValues {

  // "BrowserFetch" should "create empty headers" in {
  //   assert(emptyHeaders.toSet.size == 0)
  // }

  // it should "convert to http headers" in {
  //   val x = toHttpHeaders(newHeaders(js.Dictionary[String](
  //     "blah" -> "hah"
  //   )))
  //   assert(x("blah") == "hah")
  // }

  // it should "convert to fetch headers" in {
  //   val x = toFetchHeaders(HttpHeaders("blah" -> "hah"))
  //   val t = x.get("blah")
  //   assert(t.isDefined && t.map(_ == "hah").getOrElse(false))
  // }

  // it should "convert RequestInit headers" in {
  //   succeed
  // }

}
