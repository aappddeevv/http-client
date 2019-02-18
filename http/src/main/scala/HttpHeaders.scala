// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg
package client
package http

trait HeaderKeys {
  val Accept = "Accept"
  val AcceptCharset = "Accept-Charset"
  val AcceptEncoding = "Accept-Encoding"
  val AcceptLanguage = "Accept-Language"  
  val Authorization = "Authorization"
  val CacheControl = "Cache-Control"
  val ContentLength = "Content-Length"
  val ContentType = "ContentType"
  val ContentID = "Content-ID"
  val ETag = "ETag"
  val Expect = "Expect"
  val Host = "Host"
  val IfMatch = "If-Match"
  val IfNoneMatch = "If-None-Match"
  val Origin = "Origin"
  val SetCookie = "Set-Cookie"
  val UserAgent = "User-Agent"
}

object HttpHeaders extends HeaderKeys {
  /** HttpHeaders zero. */
  val empty: HttpHeaders = collection.immutable.Map[String, Seq[String]]()
  def apply(p: (String, String)*): HttpHeaders = p.toMap.mapValues(Seq(_))
  def seqPairs(p: (String, Seq[String])*): HttpHeaders = p.toMap
  //def pairs(p: (String, String)*): HttpHeaders = apply(p:_*)
  def map(p: Map[String,String]): HttpHeaders = p.mapValues(Seq(_))

  def contentId(id: String) = apply(ContentID -> id)
  def contentType(t: String) = apply(ContentType -> t)

  /** Render to a String. Newline is added on end if any content is rendered. */
  def render(h: HttpHeaders): String = {
    val sb = new StringBuilder()
    h.foreach {
      case (k, arr) =>
        val v = h(k).mkString(";")
        sb.append(s"$k: $v\r\n")
    }
    sb.toString()
  }
}

