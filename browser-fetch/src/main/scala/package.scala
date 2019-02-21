// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg
package client

import org.scalajs.dom
import dom.experimental._
import cats._
import cats.data._

import ttg.client.http._

package object browserfetch {
  type FetchResponse = dom.experimental.Response

  def request(method: Method, path: String, headers: HttpHeaders, body: Id[BodyInit]): HttpRequest[Id,BodyInit] =
    HttpRequest[Id,BodyInit](
      method = method,
      path = path,
      headers = headers,
      body = body)

  def get(path: String, headers: HttpHeaders = HttpHeaders.empty) =
    HttpRequest[Id,BodyInit](Method.GET, path, headers = headers, body="")
}
