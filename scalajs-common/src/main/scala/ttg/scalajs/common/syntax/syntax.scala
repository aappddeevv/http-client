// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg
package scalajs
package common

import scala.scalajs.js
import js._
import js.|
import JSConverters._
import io.scalajs.nodejs._
import scala.concurrent._
import io.scalajs.util.PromiseHelper.Implicits._
import fs2._
import cats._
import cats.data._
import cats.implicits._
import cats.arrow.FunctionK
import cats.effect._
import js.Dynamic.{literal => jsobj}

import Utils._

final case class StreamOps[F[_], O](s: Stream[F, O]) {
  def vectorChunkN(n: Int): Stream[F, Vector[O]] =
    s.chunkN(n).map(_.toVector)
  def groupBy[O2](f: O => O2)(implicit eq: Eq[O2]): Stream[F, (O2, Vector[O])] =
    s.groupAdjacentBy(f).map(p => (p._1, p._2.toVector))
}

trait StreamSyntax {
  implicit def streamToStream[F[_], O](s: Stream[F, O]): StreamOps[F, O] = StreamOps(s)
}

object NPMTypes {
  type JSCallbackNPM[A] = js.Function2[io.scalajs.nodejs.Error, A, scala.Any] => Unit
  type JSCallback[A]    = js.Function2[js.Error, A, scala.Any] => Unit

  /** This does not work as well as I thought it would... */
  def callbackToIO[A](f: JSCallbackNPM[A])(implicit e: ExecutionContext): IO[A] = JSCallbackOpsNPM(f).toIO
}

import NPMTypes._

final case class JSCallbackOpsNPM[A](val f: JSCallbackNPM[A]) {

  import scala.scalajs.runtime.wrapJavaScriptException

  /** Convert a standard (err, a) callback to a IO. */
  def toIO(implicit e: ExecutionContext) =
    IO.async { (cb: (Either[Throwable, A] => Unit)) =>
      f((err, a) => {
        if (err == null || js.isUndefined(err)) cb(Right(a))
        else cb(Left(wrapJavaScriptException(err)))
      })
    }
}

trait JSCallbackSyntaxNPM {
  implicit def jsCallbackOpsSyntaxNPM[A](f: JSCallbackNPM[A])(implicit s: ExecutionContext) = JSCallbackOpsNPM(f)
}

trait JsObjectInstances {

  /** Show JSON in its rawness form. Use PrettyJson. for better looking JSON. */
  implicit def showJsObject[A <: js.Object] = Show.show[A] { obj =>
    val sb = new StringBuilder()
    sb.append(JSON.stringify(obj)) //Utils.pprint(obj))
    sb.toString
  }

  /**
    * Monoid for js.Object, which is really just a dictionary. Use |+| or "combine" to
    * combine.
    */
  implicit val jsObjectMonoid: Monoid[js.Object] =
    new Monoid[js.Object] {
      def combine(lhs: js.Object, rhs: js.Object): js.Object = {
        Utils.merge[js.Object](lhs, rhs)
      }

      def empty: js.Object = new js.Object()
    }
}

trait IOInstances {
  val ioToIOFunctionK = FunctionK.id[IO]

  implicit val ioToIO: IO ~> IO = new (IO ~> IO) {
    override def apply[A](ioa: IO[A]): IO[A] = ioa
  }
}

trait JsPromiseInstances {
  implicit def ttgJsPromisetoF[F[_]: Async, A](p: js.Promise[A]): F[A] =
    common.jsPromiseToF[F](Async[F])(p)
}

trait JsPromiseSyntax {
  implicit class RichPromise[A](p: js.Promise[A]) {
    /** Requires an `Async[F]` which forces error to subclass of Throwable. */
    def toF[F[_]](implicit F: Async[F]): F[A] = common.jsPromiseToF[F](F)(p)
    def toIO(implicit A: Async[IO]): IO[A] = toF[IO]
  }
}

case class FutureOps[A](val f: Future[A])(implicit ec: ExecutionContext) {
  def toIO: IO[A] = IO.fromFuture(IO(f))
}

trait FutureSyntax {
  implicit def futureToIO[A](f: Future[A])(implicit ec: ExecutionContext) = FutureOps[A](f)
}

case class IteratorOps[A](val iter: scala.Iterator[A])(implicit ec: ExecutionContext) {

  /** I think this is fs2 now directly: `Stream.fromIterator`. */
  def toFS2Stream[A] = Stream.unfold(iter)(i => if (i.hasNext) Some((i.next, i)) else None)
}

trait IteratorSyntax {
  implicit def toIteratorOps[A](iter: scala.Iterator[A])(implicit ec: ExecutionContext) =
    IteratorOps[A](iter)
}

// Add each individual syntax trait to this
trait AllSyntax
    extends JsDynamicSyntax
    with JSCallbackSyntaxNPM
    with JsUndefOrSyntax
    with JsObjectSyntax
    with JsAnySyntax
    with FutureSyntax
    with IteratorSyntax
    with JsPromiseSyntax
    with StreamSyntax
    with OrNullSyntax

// Add each individal syntax trait to this
object syntax {
  object all           extends AllSyntax
  object jsdynamic     extends JsDynamicSyntax
  object jscallbacknpm extends JSCallbackSyntaxNPM
  object jsundefor     extends JsUndefOrSyntax
  object jsobject      extends JsObjectSyntax
  object jsany         extends JsAnySyntax
  object future        extends FutureSyntax
  object iterator      extends IteratorSyntax
  object jspromise extends JsPromiseSyntax
  object stream    extends StreamSyntax
  object ornull    extends OrNullSyntax
}

trait AllInstances extends JsObjectInstances

object instances {
  object all extends AllInstances
  object jsobject extends JsObjectInstances
  object jspromise extends JsPromiseInstances
  object io        extends IOInstances
}

object implicits extends AllSyntax with AllInstances
