// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg.odata
package client

import cats._
import cats.implicits._
import fs2._

import http._

/**
 * Simple OData client based on the contents of this package. You can use this directly
 * for simple OData client needs based on a generic OData server.
 */
object SmpleODataClient {

  import HeaderRenderer._

  def apply[F[_]](httpClient: client.http.Client[F], baseUrl: String)(
  implicit M: MonadError[F, Throwable], C: Stream.Compiler[F,F]): ODataClient[F] =
    new ODataClient[F] {
      type PreferOptions = BasicPreferOptions
      type RequestOptions = BasicRequestOptions[PreferOptions]

      implicit protected val compiler = C
      implicit val F = M
      val http = httpClient
      val base = baseUrl
      private val prenderer = HeaderRenderer[PreferOptions]
      val optRenderer = HeaderRenderer.instance[RequestOptions](opts => prenderer(opts.prefers))

      def raiseError[A](resp: HttpResponse[F], msg: String, req: Option[HttpRequest[F]]): F[A] =
        F.flatMap(resp.body.content){ str =>
          F.raiseError(new IllegalArgumentException("Unknown error--insert more code here to fluff out the error."))
        }
    }
}

