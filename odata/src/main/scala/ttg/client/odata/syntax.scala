// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg
package client
package odata

final case class IdOps[A <: ODataId](val id: A) extends AnyVal {
  def asString(implicit r: IdRenderer[A]): String = r.render(id)
}

final case class IdStringOps(val s: String) extends AnyVal {
  def oDataId: ODataId = Id(s)
}

trait IdSyntax {
  implicit def odataSyntaxODataId(id: ODataId) = new IdOps(id)
  implicit def odataIdStringOps(s: String)           = new IdStringOps(s)
}

trait AllSyntax
    extends QuerySpecSyntax
    with IdSyntax

object syntax {
  object all        extends AllSyntax
  object queryspec  extends QuerySpecSyntax
  object odataid extends IdSyntax
}

trait AllInstances
    extends IdRenderableInstances
    with DecoderInstances

object instances {
  object all    extends AllInstances
  object id extends IdRenderableInstances
  object odatadecoders extends DecoderInstances
}

object implicits extends AllSyntax with AllInstances
