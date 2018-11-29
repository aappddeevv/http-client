// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg.odata
package client

import scala.scalajs.js
import js.annotation._

sealed trait ODataId
case class Id(id: String, name: Option[String] = None) extends ODataId
/** Alternate key. You'll need to quote the value in the string, if that's needed. */
case class AltId(parts: Seq[(String, String)]) extends ODataId

object AltId {
  def apply(e: String, id: String): AltId = AltId(Seq((e, id)))
}

trait ODataIdRenderable[A <: ODataId] {
  def render(id: A): String
}

object ODataIdRenderable extends ODataIdRenderableInstances {
  def apply[A <: ODataId](implicit r: ODataIdRenderable[A]) = r
}

trait ODataIdRenderableInstances {
  implicit val idRenderable = new ODataIdRenderable[Id] {
    def render(id: Id) = id.name.map(n => s"$n = $id").getOrElse(id.name)
  }
  
  implicit val altIdRenderable = new ODataIdRenderable[AltId] {
    def render(id: AltId) = id.parts.map(p => s"${p._1} = ${p._2}").mkString(",")
  }  
}
