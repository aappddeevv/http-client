// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg.odata
package client

import scala.scalajs.js
import js._
import cats._
import cats.effect._
import fs2._

import http._
import client.syntax.queryspec._

/** OData operations for getting a single entity. */
trait GetOneOps[F[_]] {
  self: ClientError[F]
      with HttpResources[F]
      with ClientFConstraints[F]
      with ClientRequests[F]
      with ClientIdRenderer =>

  /**
    * Get a single entity using key information. The key can be a guid or
    * alternate key criteria e.g "altkeyattribute='Larry',...". You get all the
    * fields with this. This assumes that the entity exists since you providing
    * it in id--you should know ahead of time.
    *
    * Allow you to specify a queryspec somehow as well!?!?
    */
  def getOneWithKey[A](entitySet: String,
                       key: ODataId,
                       attributes: Seq[String] = Nil,
    opts: Option[RequestOptions] = None)(
    implicit d: EntityDecoder[F, A]): F[A] = {
    val q = QuerySpec(select = attributes)
    getOne(q.url(s"/$entitySet(${renderId(key)})"), opts)(d)
  }

  /**
    * Get one entity using a full query url. You can use a QuerySpec to form the
    * url then call `qs.url(myentities,Some(theid))`.
    *
    * If you use a URL that returns a OData response with a `value` array that
    * needs be automatically extracted, you need to use an explict EntityDecoder
    * that first looks for that array then obtains your `A`. See
    * `EntityDecoder.ValueWrapper` for an example.
    *
    * You also use this pattern when employing `getOne` to obtain related
    * records in a 1:M navigation property e.g. a single entity's set of
    * connections or some child entity. In this case, your URL will typically
    * have an "expand" segment.  Note that if you navigate to a simple
    * attribute, then it is returned as a simple object also attached to "value"
    * so choose your decoder wisely.
    *
    */
  def getOne[A](url: String, opts: Option[RequestOptions]=None)(
      implicit d: EntityDecoder[F, A]): F[A] = {
    val request = HttpRequest[F](Method.GET, url, headers = toHeaders(opts), Entity.empty[F])
    http.fetch(request) {
      case Status.Successful(resp) => resp.as[A]
      case failedResponse =>
        raiseError(failedResponse, s"Get one entity $url", Option(request))
    }
  }

}
