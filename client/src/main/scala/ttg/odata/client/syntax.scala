// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg.odata
package client

final case class ODataIdOps[A <: ODataId](val id: A) extends AnyVal {
  def asString(implicit r: ODataIdRenderable[A]): String = r.render(id)
}

final case class ODataIdStringOps(val s: String) extends AnyVal {
  def oDataId: ODataId = Id(s)
}

trait ODataIdSyntax {
  implicit def odataSyntaxODataId(id: ODataId) = new ODataIdOps(id)
  implicit def odataIdStringOps(s: String)           = new ODataIdStringOps(s)
}

trait AllSyntax
    extends QuerySpecSyntax
    with ODataIdSyntax

object syntax {
  object all        extends AllSyntax
  object queryspec  extends QuerySpecSyntax
  object odataid extends ODataIdSyntax
}

trait AllInstances extends ODataIdRenderableInstances

object instances {
  object all    extends AllInstances
  object id extends ODataIdRenderableInstances 
}

object implicits extends AllSyntax with AllInstances
