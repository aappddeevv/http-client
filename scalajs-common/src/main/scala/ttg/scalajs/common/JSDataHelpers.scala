// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg.scalajs
package common

import scala.scalajs.js
import js._
import js.Dynamic.{literal => jsobj}
import JSConverters._

import ttg.scalajs.common
import common._
import common.implicits._

/**
 * Helpers when working with scalajs data, specifically, js.Object, js.Dynamic and
 * js.Dictionary. Most of these mutate the input source so create a copy of the data
 * prior to calling if you want.
 */
object JSDataHelpers {

  /**
   * Assuming a property is a string, omit the property from the object if its blank.
   * This is a mutating action.
   */
  @inline
  def pruneIfBlank(obj: JsAnyDict, p: String*): obj.type = {
    p.foreach { k =>
      if (isBlank(obj, k)) omit(obj, k)
    }
    obj
  }

  /** Slow but steady deep copy using JSON.stringify and JSON.parse. */
  @inline
  def deepCopy(obj: js.Object): js.Object = JSON.parse(JSON.stringify(obj)).asInstanceOf[js.Object]

  /**
   * If the property is present and is not blank, apply f to it.
   */
  @inline
  def replaceIfBlank(obj: JsAnyDict, p: String, f: js.Any => js.Any): obj.type =
    xfObj[js.Any](obj, p, {
      case a if (isBlank(obj, p)) => obj += (p -> f(a))
    })

  /** True if property is defined on the object. Shouldn't we use hasOwnProperty? */
  @inline
  def isDefined(obj: js.Dictionary[_], p: String) = if (obj.contains(p)) true else false

  /** If property is defined, do something to object, otherwise return original object. Mutating! */
  @inline
  def doIfDefined(obj: JsAnyDict, p: String, f: js.Dictionary[js.Any] => Unit): obj.type = {
    if (isDefined(obj, p)) f(obj)
    obj
  }

  /** If property is present, return true if its value is js truthy true else return false. */
  @inline
  def isBlank(obj: JsAnyDict, p: String): Boolean =
    obj.contains(p) && !DynamicImplicits.truthValue(obj(p).asDyn)

  /** If property is present, replace with value regardless of the current value. */
  @inline
  def setTo(obj: JsAnyDict, p: String, value: js.Any): obj.type =
    doIfDefined(obj, p, { dict =>
      dict += (p -> value)
    })

  @inline
  def roundAt(p: Int)(n: Double): Double = {
    val s = math pow (10, p)
    (math round n * s) / s
  }

  /**
   * Get a value in the js object and return it wrapped in `Option`. If found,
   * it is removed from the object, a mutating operation.
   */
  @inline
  def getAndZap[A](j: JsAnyDict, p: String): Option[A] = {
    val r = j.get(p).asInstanceOf[Option[A]]
    r.foreach(_ => j -= p)
    r
  }

  /** Removes keys if present. Mutates input object. lodash naming. */
  @inline
  def omit(j: js.Dictionary[_], keys: String*): j.type = {
    keys.foreach { j.remove(_) }
    j
  }

  /** Removes keys if the field name contains the pattern substring. This does not
   * use regex.
   * @todo Make this more efficient.
   */
  @inline
  def omitIfMatch(j: js.Dictionary[_], patterns: Seq[String]): j.type = {
    def contains(a: String): Boolean = patterns.map(a.contains(_)).collect{ case x if x => x}.length >0
    val deletes = j.keys.map(n => (contains(n), n)).collect { case (f, n) if f => n }
    omit(j, deletes.toSeq:_*)
  }

  @inline
  def keepOnly(obj: JsAnyDict, names: String*): obj.type = {
    updateObject((obj.keySet -- names).toSeq, Nil, obj)
    obj
  }

  /** Rename keys. Mutates input object. Old keys are removed as they
    * are moved. If a key is not found, that property is not renamed.
    */
  @inline
  def rename(j: JsAnyDict, renames: (String, String)*): j.type = {
    renames.foreach {
      case (o, n) =>
        j.get(o) match {
          case Some(v) =>
            j -= o
            j(n) = v
          case None => // do nothing
        }
    }
    j
  }

  /** Update an object. Mutates in place! Processng order is drops then renames. */
  @inline
  def updateObject[A](drops: Seq[String], renames: Seq[(String, String)], obj: JsAnyDict): js.Dictionary[js.Any] = {
    val d = obj.asInstanceOf[js.Dictionary[js.Any]]
    omit(d, drops: _*)
    rename(d, renames: _*)
    obj
  }

  /** If property is present, apply partial function. */
  @inline
  def xfObj[A](obj: JsAnyDict, p: String, pf: PartialFunction[A, Unit]): obj.type =
    doIfDefined(obj, p, dict => {
      if (dict.contains(p)) {
        val a = dict(p).asInstanceOf[A]
        if (pf.isDefinedAt(a)) pf.apply(a)
      }
    })

  /**
    * Given some string definitions of keep, drop and rename, create a function that
    * that mutates a js.Object. This parses the "dsl" and returns a function to
    * perform the conversion without needing to reparse the "dsl".
    */
  def stdConverter(
    keeps: Option[String] = None,
    drops: Option[String] = None,
    renames: Option[String] = None): js.Object => js.Object = {
    val k = keeps.map(_.split('|').toSeq.map(_.trim))
    val d = drops.map(_.split('|').toSeq.map(_.trim))
    val r: Option[Seq[(String, String)]] =
      renames.map(_.split('|').map(_.split("->")).map(arr => (arr(0).trim, arr(1).trim)))
    mutate(k, d, r)
  }

  /** Mutate an object. Use `Option` for parameters instead of `Nil` to short
   * circuit a few function calls.
   */
  @inline
  def mutate(
    keeps: Option[Seq[String]] = None,
    drops: Option[Seq[String]] = None,
    renames: Option[Seq[(String,String)]] = None): js.Object => js.Object =
    obj => {
        val o = obj.asAnyDict
        drops.foreach(omit(o, _: _*))
        renames.foreach(JSDataHelpers.rename(o, _: _*))
        keeps.foreach(keepOnly(o, _: _*))
        obj
    }

}
