// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg
package client
package browserfetch

import scala.scalajs.js
import js.|
import org.scalajs.dom
import dom.experimental._
import dom._
import scala.scalajs.js.typedarray.{ArrayBuffer, Uint8Array}

import cats._
import cats.data._
import cats.implicits._
import cats.effect._
import js.JSConverters._

import ttg.scalajs.common
import client.http._
import common.implicits._

/*
 val myByteArray = new ArrayBuffer(100)
 val request = Reqest(Method.GET, mypath, myByteArray)
 val x: js.Promise[js.Object] = fetch(path, options).then(_.json())
 */

// encoders for standard types
object encoders {
  // allowed fetch request body objects: ArrayBuffer, ArrayBufferView, Blob/File, String, URLSearchParams, FormData

  /** Domain String to backend String. */
  def stringEncoder[B[_]: Applicative] =
    EntityEncoder.instance[B, String, String] { a =>
      (HttpHeaders.empty, Applicative[B].pure(a))
    }

  /** Domain `.toString` to backend String. */
  def toStringEncoder[B[_]: Applicative] =
    EntityEncoder.instance[B, scala.Any, String] { obj =>
      (HttpHeaders.empty, Applicative[B].pure(obj.toString))
    }

  /** Any subclass of js.Object to backend String using JSON.stringify. */
  def jsonEncoder[B[_]: Applicative]/*(replacer, space)*/ =
    EntityEncoder.instance[B, js.Object, String] { obj =>
      (HttpHeaders.JsonHeaders, Applicative[B].pure(js.JSON.stringify(obj)))
    }
}

/** Decoders for standard types, browser fetch client wraps the raw Response
 * object in order to allow its semantics to bubble up to the decoder level.  So
 * "message" is the scala HttpResponse and "message.body" is a FetchResponse.
 */
object decoders {
  def blob[F[_]: Async] =
    EntityDecoder.instance[Id, Response, F, Blob]{ message =>
      DecodeResult.success[F, Blob](message.body.blob().toF[F])
    }

  /** A null body, which is allowed, is mapped to the empty string `""`. */
  def string[F[_]: Async] =
    EntityDecoder.instance[Id, Response, F, String]{ message =>
      DecodeResult.success(message.body.text().toF[F].map(maybeNull =>
        if(maybeNull == null) "" else maybeNull
      ))
    }

  def fastJson[F[_]: Async, T <: js.Any] =
    EntityDecoder.instance[Id, Response, F, T]{ message =>
      DecodeResult.success(message.body.json().toF[F].map(_.asInstanceOf[T]))
    }

  def json[F[_]: Async, T <: js.Any](reviver: Option[Reviver] = None) =
    string.map(js.JSON.parse(_, reviver.getOrElse(common.undefinedReviver)).asInstanceOf[T])

  def mustHaveStatus[F[_]: Async](status: Status, msg: Option[String]=None) =
    EntityDecoder.instance[Id, Response, F, Boolean]{ message =>
      if(message.status == status) DecodeResult.success(true)
      else string.flatMapR(bodyStr =>
        DecodeResult.failure[F, Boolean](new UnexpectedHttpStatus(
          status,
          Option(s"""${msg.getOrElse("")}\n$bodyStr""")
        )))(message)
    }

  def readableStream[F[_]: Async] =
    EntityDecoder.instance[Id, Response, Id, ReadableStream[Uint8Array]] { message =>
      DecodeResult.success(message.body.body)
    }
}


object ClientUtils {

  /** Combine headers, return a newly allocated Header. `append` vs `set` is used.
   */
  implicit val headersSemigroup =
    new Semigroup[Headers] {
      def combine(lhs: Headers, rhs: Headers): Headers = {
        val result = new Headers(lhs)
        for(kvarray <- rhs) result.append(kvarray(0), kvarray(1))
        result
      }}

  /** Zero for Headers. Treat as immutable even though it's not. */
  lazy val emptyHeaders: Headers = new Headers()
  lazy val emptyRequestInit: RequestInit = RequestInit()

  /** Convert HtppHeaders to Headers. */
  def toFetchHeaders(h: HttpHeaders): Headers = {
    val r = new Headers()
    for((k,v) <- h) r.set(k,v.mkString(","))
    r
  }

  def toHttpHeaders(h: Headers): HttpHeaders =
    HttpHeaders(h.map(entry => (entry(0), entry(1))).toSeq:_*)

  def toHeaders(h: HeadersInit): Headers =
    h match {
      case headers@_ if headers.isInstanceOf[Headers] => headers.asInstanceOf[Headers]
      case headers@_ if js.typeOf(headers.asInstanceOf[js.Any]) == "object" =>
        val r = new Headers()
        for((k,v) <- headers.asInstanceOf[js.Dictionary[String]]) r.set(k,v)
        r
      case headers@_ if js.Array.isArray(headers.asInstanceOf[js.Any]) =>
        val r = new Headers()
        for(a <- headers.asInstanceOf[js.Array[js.Array[String]]]) r.set(a(0), a(1))
        r
      case _ => new Headers()
    }

  def combine(lhs: RequestInit, rhs: RequestInit): RequestInit =
    common.Utils.merge[RequestInit](lhs, rhs)

}

/**
 * Client based on browser `fetch`.
 * 
 * @see https://developer.mozilla.org/en-US/docs/Web/API/WindowOrWorkerGlobalScope/fetch
 */
object Client {
  import ClientUtils._

  /** Create client based on the browser's fetch function. If a url base is not
   * provided it defaults to the document's location. This method formulates the
   * browser fetch Request object. Non fatal exceptions are turned into
   * CommunicationsFailure and returned in F's error channel.
   * 
   * @param base Base URL prepended to all requests. Default is document.location.origin.
   * @param convert Natural transformation from js.Promise -> F. Default is common.jsPromiseToF[F]
   * @param baseRequestInit Request parameters used in all requests. run's
   * request overrides and there are no smart combines. Default is None.
   * @tparam F Async effect. In cats this forces E to Throwable.
   */
  def apply[F[_]: Async](
    convert: Option[js.Promise ~> F] = None,
    base: Option[String] = None,    
    baseRequestInit: Option[RequestInit] = None
  ) = {

    val F = Async[F]
    val convertEffect = convert.getOrElse(common.jsPromiseToF[F])
    val url =
      base
        .map(u => if(u.endsWith("/")) u.dropRight(1) else u)
        .getOrElse(dom.document.location.origin)

    http.Client[Id,BodyInit,Id,FetchResponse,F,Throwable] { request =>
      val hashttp = request.path.startsWith("http")
      // this should not be assert() here, this is out of the error channel!
      assert(request.path(0) == '/' || hashttp,
        s"Request path must start with a slash (/) or http: ${request.path}}")
      val path                        = (if (!hashttp) url else "") + request.path

      // merge RequestInits taking care to merge headers correctly
      val fetchopts = combine(
        baseRequestInit.getOrElse[RequestInit](emptyRequestInit),
        RequestInit(
          body = if(request.method==Method.GET) js.undefined else request.body,
          headers =
            baseRequestInit.flatMap(_.headers.map(toHeaders(_)).toOption).getOrElse(emptyHeaders) |+|
              toFetchHeaders(request.headers),
          method = request.method.asString.asInstanceOf[HttpMethod]
        ))
      val responseF = convertEffect(Fetch.fetch(path, fetchopts)).map { r =>
        HttpResponse[Id, Response](
          status = Status.lookup(r.status),
          headers = toHttpHeaders(r.headers),
          body = r
        )
      }
      // Have effect with remote call in it and a returned response add some
      // error recovery to map into the F effect's error channel with
      // CommunicationsFailure.
      responseF
        .handleErrorWith {
          case scala.util.control.NonFatal(t: js.JavaScriptException) =>
            val x = t.exception.asInstanceOf[js.Any]
            //dom.console.log(s"browserfetch.Client: nonfatal error $t: [", x, "], ", js.typeOf(x))
            F.raiseError(new CommunicationsFailure(
              s"browser fetch client caught a JS exception, details: ${x.toString}",
              Option(t)
            ))
        }
    }

    // // create new client
    // new http.Client[Id, BodyInit, Id, FetchResponse, F, Throwable] {
    //   def run[A, T](
    //     request: HttpRequest[Id, A])(
    //     whenHTTPStatusError: HttpResponse[Id, FetchResponse] => DecodeResult[F,T])(
    //     implicit
    //       encoder: EntityEncoder[Id, A, BodyInit],
    //       decoder: EntityDecoder[Id, FetchResponse, F, T]
    //   ): DecodeResult[F,T] = {
    //     val hashttp = request.path.startsWith("http")
    //     // this should not be assert() here, this is out of the error channel!
    //     assert(request.path(0) == '/' || hashttp,
    //       s"Request path must start with a slash (/) or http: ${request.path}}")
    //     val path                        = (if (!hashttp) url else "") + request.path

    //     type X = (HttpHeaders, Id[BodyInit])
    //     val (headers, fetchRequestBody) = encoder(request.body)

    //     // merge RequestInits taking care to merge headers correctly
    //     val fetchopts = combine(
    //       baseRequestInit.getOrElse[RequestInit](emptyRequestInit),
    //       RequestInit(
    //         body = if(request.method==Method.GET) js.undefined else fetchRequestBody,
    //         headers =
    //           baseRequestInit.flatMap(_.headers.map(toHeaders(_)).toOption).getOrElse(emptyHeaders) |+|
    //             toFetchHeaders(request.headers),
    //         method = request.method.asString.asInstanceOf[HttpMethod]
    //       ))
    //     val responseF = convertEffect(Fetch.fetch(path, fetchopts)).map { r =>
    //       HttpResponse[Id, Response](
    //         status = Status.lookup(r.status),
    //         headers = toHttpHeaders(r.headers),
    //         body = r
    //       )
    //     }
    //     // use decoder, doesn't this cause a couple extra allocations?
    //     DecodeResult(F.flatMap(responseF)(_.attemptAs[F,T].value))
    //   }
    // }
  }
}
