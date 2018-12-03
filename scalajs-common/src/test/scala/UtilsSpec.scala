// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg.scalajs
package common

import scala.scalajs.js
import js.Dynamic.{literal => jsobj}
import org.scalatest._

trait Test1 extends js.Object {
  var blah: js.UndefOr[String] = js.undefined
}

trait Test2 extends js.Object {
  var blah: js.UndefOr[js.Date] = js.undefined
}

class UtilsSpec extends FlatSpec with Matchers with OptionValues {
  import Utils._

  "Utils" should "parse json" in {
    val x = parseJson[Test1]("""{"blah":"hah"}""")
    assert(x.blah.contains("hah"))
  }

  it should "merge js.Objects" in {
    val x = merge[Test1](
      new Test1 { blah = "first" },
      js.Dynamic.literal("notplanned" -> "hah"),
      new Test1 { blah = "lastwins"}
    )
    assert(x.blah.contains("lastwins"))
  }

  it should "merge js.Dynamic in" in {
    val x = mergeJSObjects(
      jsobj("blah" -> "first"),
      jsobj("blah" -> "lastwins"),
      jsobj("hah" -> "lastwins")
    )
    assert (
      x.blah.asInstanceOf[String] == "lastwins" &&
      x.hah.asInstanceOf[String] == "lastwins"
    )
  }

  it should "strip up to" in {
    assert(stripUpTo("blah/hah/foo/bar", "foo") == "foo/bar")
  }

  it should "strip up to nothing, with no matches" in {
    assert(stripUpTo("blah/hah/foo/bar/king", "king") == "king")
  }

  it should "parse UTC dates" in {
    val x = parseJsonWithDates[Test2]("""{"blah":"2018-10-01T06:00:00Z"}""")
    println(s"blah ${x.blah}")
    assert(x.blah.map(_.getTime()).contains(new js.Date("2018-10-01T06:00:00Z").getTime()))
  }

}

