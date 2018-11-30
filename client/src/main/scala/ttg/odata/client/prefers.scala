// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg.odata
package client

import http.HttpHeaders

/** Prefer options in OData. Translate into 'Prefer:' header and comma separated values. */
trait BasicPreferOptions {
  /** odata.allow-entityreferences */
  val allowEntityReferences: Boolean
  /** odata.callback; url="callback url" */
  val callback: Option[String]
  /* odata.continue-on-error */
  val continueOnError: Boolean
  /** FormattedValue is broken out separately. If Some(Nil) then => odata.include-annotations="*" */
  val includeAnnotations: Option[Seq[String]]
  /** odata.maxpagesize */
  val maxPageSize: Option[Int]
  /** return=representation or return=minimal. Can't use with GET or DELETE. */
  val `return`: Option[Representation]
  /** Pushed into includeAnnotations automatically. Broken out since its ofter
   * changed request to request.
   */
  val formattedValues: Boolean
  /** respond-async */
  val respondAsync: Boolean
  /** wait=X */
  val waitx: Option[Int]
}

/** Case class version if that works for your client. */
case class PreferOptions(
  allowEntityReferences: Boolean = false,
  callback: Option[String] = None,
  continueOnError: Boolean = false,
  includeAnnotations: Option[Seq[String]] = None,
  maxPageSize: Option[Int] = None,
  `return`: Option[Representation] = None,
  formattedValues: Boolean = false,
  respondAsync: Boolean = false,
  waitx: Option[Int] = None
) extends BasicPreferOptions

sealed trait Representation extends scala.Any
object Representation {
  val minimal = "minimal".asInstanceOf[Representation]
  val representation = "representation".asInstanceOf[Representation]
}

object prefer {

  /** Renders value side of BasicPreferOptions header. Does not return an
   * `HttpHeader` just a big long string to use for the "Prefer:" header.
   */
  def render(popts: BasicPreferOptions): Option[String] = {
    val opts = collection.mutable.ListBuffer[Option[String]]()

    opts ++= Seq(
      popts.maxPageSize.map(x => s"odata.maxpagesize=$x"),
      popts.`return`.map { v => s"return=${v}" },
      popts.waitx.map{w => s"wait=$w"},
      Option(popts.continueOnError).flatMap{c => if(c) Option(s"odata.continue-on-error=$c") else None},
      Option(popts.allowEntityReferences).flatMap{a => if(a) Option("odata.allow-entityreferences=$a") else None},
      popts.callback.map{u => s"""odata.callback; url="$u""""},
      Option(popts.respondAsync).flatMap(r => if(r) Option(s"respond-async") else None)
    )

    val annotations = popts.includeAnnotations match {
      case Some(Nil) => Option("""odata.include-annotations="*"""")
      case Some(list) =>
        val fulllist = list :+
        Option(popts.formattedValues).filter(identity).map(_ => headers.FormattedValue).getOrElse("")
        Option("""odata.include-annotations="${fulllist.mkString(",")}"""")
      case _ => None
    }

    val str = (opts :+ annotations).collect { case Some(x) => x }.mkString(",")
    if (str == "") None
    else Option(str)
  }
}

trait PreferInstances {
  val preferRenderer = HeaderRenderer.instance[BasicPreferOptions]{ p =>
    prefer.render(p).fold(HttpHeaders.empty)(p => HttpHeaders("Prefer" -> p))
  }
}
