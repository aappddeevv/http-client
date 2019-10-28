// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg
package scalajs
package common
package cats

// Add each individual syntax trait to this
trait AllSyntax
    extends FutureSyntax
    with IteratorSyntax
    with JsPromiseSyntax
    with StreamSyntax

// Add each individal syntax trait to this
object syntax {
  object all           extends AllSyntax
  object future        extends FutureSyntax
  object iterator      extends IteratorSyntax
  object jspromise extends JsPromiseSyntax
  object stream    extends StreamSyntax
}
