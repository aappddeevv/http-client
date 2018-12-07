// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg.odata.client
package http

import scala.concurrent.duration._
import scala.scalajs.js
import scala.concurrent._
import fs2._
import cats._
import cats.data._
import cats.implicits._
import cats.effect._

trait RetryPolicies {
  import Status._

  //protected implicit val timer = odelay.js.JsTimer.newTimer

  protected[this] val RetriableStatuses = Set(
    RequestTimeout,
    /**
      * This is problematic, could be badly composed content! but then would we retry?
      * May have to read error to know which toasts everyone downstream of the retry.
      */
    InternalServerError,
    ServiceUnavailable,
    BadGateway,
    GatewayTimeout,
    TooManyRequests
  )

  def log(msg: String): Unit = println(msg)

  /**
    * Return true if the status *suggests* that the request should be retried.
    * The statuses include: InternalServerError, ServiceUnavailable, BadGateway,
    * GatewayTimeout, TooManyRequests which is a good starting point for SaaS
    * OData endpoints.
    */
  def isRetryableStatus(s: Status): Boolean = RetriableStatuses.contains(s)

  /**
   * Detect a few specific Throwables that indicate retry.
   */
  def retryIfIsRetryableThrowable(t: Throwable): Boolean =
    t match {
      case c: CommunicationsFailure                                          => true
      case d: MessageBodyFailure                                             => true
      case _ => false
    }

  /**
   * Test the throwable error or the response status to see if a retry shouldu be tried.
   * Uses `etryIfIsRetryableThrowable` and `isRetryabelStatus`.
   */
  def shouldRetry[F[_]](v: Either[Throwable, HttpResponse[F]]): Boolean =
    v match {
      case Right(r) => isRetryableStatus(r.status)
      case Left(t) => retryIfIsRetryableThrowable(t)
    }

  /**
    * Extract a message from `t` via "getMessage" or the underlying "cause"
    * exception. Use fallback if neither of those found.
   * @todo Move this somewhere else.
    */
  def getMessage(t: Throwable, fallback: String = "no message"): String = {
    (Option(t.getMessage()) orElse Option(t.getCause()).map(_.getMessage()) orElse Option(t.toString()))
      .getOrElse(fallback)
  }

  def backoffPolicy[F[_]](initialDelay: FiniteDuration = 5.seconds, maxRetries: Int = 5): RetryPolicy[F] =
  {(req, eresp, i) =>
    val sretry = shouldRetry(eresp)
    if(sretry && i == 0 && maxRetries > 0) Some(initialDelay)
    else if(sretry && i < maxRetries) Some(initialDelay * i.toLong)
    else None
  }

  def pausePolicy[F[_]](delay: FiniteDuration = 5.seconds, maxRetries: Int = 3): RetryPolicy[F] =
  {(req, eresp, i) =>
    val sretry = shouldRetry(eresp)
      if(sretry && i < maxRetries) Some(delay)
      else None
    }

  def directlyPolicy[F[_]](maxRetries: Int = 3): RetryPolicy[F] =
  {(req, eresp, i) =>
    val sretry = shouldRetry(eresp)
    if(sretry && i < maxRetries) Some(FiniteDuration(0, MILLISECONDS))
    else None
  }
}

/**
  * Implement retry transparently for a Client as Middleware.
 * 
 * @todo: Add a throttler.
  *
  *  The new dynamics governer limits are in place and these retry policies take
  *  them into account via status TooManyRequests:
  *  @see https://docs.microsoft.com/en-us/dynamics365/customer-engagement/developer/api-limits
  */
object retry extends RetryPolicies {

  /**
    * Your effect/domain object must be able to express a Throwable "error
    * concept" in order to detect an error and then create a retry. The retry
    * strategy filters on different types of errors (exceptions, status,
    * headers, request method, etc.) so that not every `F` error causes a retry.
    */
  def makeMiddleware[
    F[_]: MonadError[?[_],E]: ErrorChannel[?[_],E],
    E <: Throwable
  ](
    policy: RetryPolicy[F])(
    implicit T: Timer[F]): Middleware[F,E] =
    client => {
      def runone(req: HttpRequest[F], attempts: Int): F[HttpResponse[F]] =
        MonadError[F,E].attempt(client.run(req)).flatMap {
          case right @ Right(response) =>
            policy(req, right, attempts) match {
              case Some(duration) =>
                T.sleep(duration) *> runone(req, attempts+1)
              case _ => MonadError[F,E].pure(response)
            }

          case left @ Left(e) =>
            policy(req, left, attempts) match {
              case Some(duration) =>
                T.sleep(duration) *> runone(req, attempts+1)
              case _ => ErrorChannel[F,E].raise(e)
            }
        }
      Client(runone(_,1))
    }

  def pause[
    F[_]: MonadError[?[_],E]: ErrorChannel[?[_],E]:Timer[?[_]],
    E<:Throwable
  ](
    maxRetries: Int = 5,
    delayBetween: FiniteDuration = 5.seconds) =
    makeMiddleware[F,E](pausePolicy(delayBetween, maxRetries))

  def directly[
    F[_]:MonadError[?[_],E]:ErrorChannel[?[_],E]:Timer[?[_]],
    E<:Throwable
  ](
    maxRetries: Int = 5) =
    makeMiddleware[F,E](directlyPolicy(maxRetries))

  def backoff[
    F[_]: Timer[?[_]]:MonadError[?[_],E]:ErrorChannel[?[_],E],
    E<:Throwable
  ](
    maxRetries: Int = 5,
    initialDelay: FiniteDuration = 5.seconds) =
    makeMiddleware[F,E](backoffPolicy(initialDelay, maxRetries))
}
