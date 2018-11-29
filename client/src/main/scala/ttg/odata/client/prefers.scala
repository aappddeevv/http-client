// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg.odata
package client

import http.{EmptyHeaders,HttpHeaders}

/** Prefer options. */
trait BasicPreferOptions { 
  def maxPageSize: Option[Int] = None
  def includeRepresentation: Option[Boolean] = None
  def includeFormattedValues: Option[Boolean] = None
}

object prefer {

  /** Renders value side of BasicPreferOptions header. Does not return an
   * `HttpHeader`.
   */
  def render(popts: BasicPreferOptions): Option[String] = {
    val opts = collection.mutable.ListBuffer[Option[String]]()

    opts += popts.maxPageSize.map(x => s"odata.maxpagesize=$x")
    opts += popts.includeRepresentation.flatMap {
      _ match {
        case true => Option("return=representation")
        case _    => None
      }
    }

    val preferExtra = Seq(
      popts.includeFormattedValues.flatMap(f => if (f) Some(headers.FormattedValue) else None)
    )
      .collect { case Some(x) => x }

    if (preferExtra.size == 3) opts += Some("odata.include-annotations=\"*\"");
    else if (preferExtra.size != 0) opts += Some("odata.include-annotations=\"" + preferExtra.mkString(",") + "\"")

    val str = opts.collect { case Some(x) => x }.mkString(",")
    if (str == "") None
    else Some(str)
  }
}

trait PreferInstances {
  val preferRenderer = HeaderRenderer.instance[BasicPreferOptions]{ p =>
    prefer.render(p).fold(EmptyHeaders)(p => HttpHeaders("Prefer" -> p))
  }
}
