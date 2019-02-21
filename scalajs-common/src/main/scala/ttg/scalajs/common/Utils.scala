// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg
package scalajs
package common

import java.io._
import scala.scalajs.js
import js.|
import scala.concurrent._
import cats.effect._
import cats._
import cats.data._
import cats.implicits._

object Utils {

  /** Factory to create a matcher.
    * @return A => Boolean
    */
  def filterOneForMatches[A](f: A => Seq[String], filters: Traversable[String] = Nil) = {
    val counter = matchCount(filters)
    (item: A) =>
      {
        val keys   = f(item)
        val counts = keys.map(counter(_)).sum
        counts > 0
      }
  }

  /** Given a set of regex filters, return String => Int that counts matches. */
  def matchCount(filters: Traversable[String]) = {
    import scala.util.matching.Regex
    val regexList =
      if (filters.size == 0) Seq(new Regex(".*"))
      else filters.map(new Regex(_))

    (item: String) =>
      {
        regexList
          .map(_.findAllMatchIn(Option(item).getOrElse("")).size)
          .collect { case l: Int if (l > 0) => true }
          .size
      }
  }

  /**
    * Given a sequence of data items and related "use these strings for
    * comparison" values, return the data items that matched. If filters is
    * empty, every item matches. The idea is to match on some values then return
    * the "keys" for where matches occurred. The input values often come from a
    * groupby.
    */
  def filterForMatches[A](wr: Traversable[(A, Seq[String])], filters: Traversable[String] = Nil): Seq[A] = {
    val counter = matchCount(filters)
    wr.map(d => {
        val keys   = d._2
        val counts = keys.map(counter(_)).sum
        (d._1, counts > 0)
      })
      .filter(_._2)
      .toSeq
      .map(_._1)
  }

  /** Return the tail of the path from the prefix forward.  It does not matter if
    * the prefix is a directory segment or part of a filename.
    */
  def stripUpTo(path: String, prefix: String): String = {
    val idx = path.indexOf(prefix)
    if (idx >= 0) path.substring(idx)
    else path
  }

  /**
    * This is really just a Semigroup "combine" operation but it does *not* use
    * "combine" at lower levels of the structure i.e. a shallow copy. Last
    * object's fields wins. Handles null inputs. Not sure thihs is named
    * correctly should be mergeJSDynamic :-). Returns newly allocated object.
    *
    * @see https://stackoverflow.com/questions/36561209/is-it-possible-to-combine-two-js-dynamic-objects
    */
  @inline def mergeJSObjects(objs: js.Dynamic*): js.Dynamic = {
    // not js.Any? maybe keep js or scala values in here....
    val result = js.Dictionary.empty[Any] // js.Any?
    for (source <- objs) {
      for ((key, value) <- if (source != null) source.asInstanceOf[js.Dictionary[Any]] else js.Dictionary.empty[Any]) // js.Any?
        result(key) = value
    }
    result.asInstanceOf[js.Dynamic]
  }

  /**
    * Merge objects and Ts together into a newly allocated object. Good for
    * merging props with data- attributes. Handles null inputs.  See the note
    * from [[mergeJSObjects]]. Last object's fields win.
    */
  @inline def merge[T <: js.Object](objs: T | js.Dynamic*): T = {
    val result = js.Dictionary.empty[Any]
    for (source <- objs) {
      for ((key, value) <- if (source != null) source.asInstanceOf[js.Dictionary[Any]] else js.Dictionary.empty[Any])
        result(key) = value
    }
    result.asInstanceOf[T]
  }

  /** Given a throwable, convert the stacktrace to a string for printing. */
  def getStackTraceAsString(t: Throwable): String = {
    val sw = new StringWriter
    t.printStackTrace(new PrintWriter(sw))
    sw.toString
  }

  /** Strip a string of control characters Preserves some ASCII characters and
   * leaves a few whitespace control characters: newline, cr, tab.
   */
  def strip(in: String): String =
    in.replaceAll("[\\p{Cntrl}&&[^\n\t\r]]", "").replaceAll("\\P{InBasic_Latin}", "")

  def parseJson[A](content: String, reviver: Option[Reviver]=None) =
    js.JSON.parse(content, reviver.getOrElse(undefinedReviver)).asInstanceOf[A]

  def parseJsonWithDates[A](content: String, reviver: Reviver = dateReviver): A =
    parseJson(content, Option(dateReviver))
}
