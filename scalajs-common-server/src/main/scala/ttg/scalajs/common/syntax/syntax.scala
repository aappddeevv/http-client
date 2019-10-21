// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg
package scalajs
package common
package server

import scala.scalajs.js
import js._
import io.scalajs.nodejs._
import io.scalajs.util.PromiseHelper.Implicits._

trait AllSyntax
    extends JSCallbackSyntaxNPM

object syntax {
  object all           extends AllSyntax
  object jscallbacknpm extends JSCallbackSyntaxNPM
}
