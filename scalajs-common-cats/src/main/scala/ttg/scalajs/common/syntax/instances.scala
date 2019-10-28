// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg
package scalajs
package common
package cats

trait AllInstances

object instances {
  object all extends AllInstances
  object jspromise extends JsPromiseInstances
  object io        extends IOInstances
}

object implicits extends AllSyntax with AllInstances
