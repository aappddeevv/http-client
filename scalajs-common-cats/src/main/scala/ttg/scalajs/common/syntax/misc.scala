// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg
package scalajs
package common
package cats

import scala.scalajs.js
import js._
import js.|
import JSConverters._
import scala.concurrent._
import fs2._
import _root_.cats._
import _root_.cats.data._
import _root_.cats.implicits._
import _root_.cats.arrow.FunctionK
import _root_.cats.effect._
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
  /** Import this to have automatic implicit conversions. */
  implicit def ttgJsPromisetoF[F[_]: Async, A](p: js.Thenable[A]): F[A] =
    common.cats.jsPromiseToF[F](Async[F])(p)
}

trait JsPromiseSyntax {
  implicit class RichPromise[A](p: js.Thenable[A]) {
    /** Requires an `Async[F]` which forces error to subclass of Throwable :-(. */
    def toF[F[_]](implicit F: Async[F]): F[A] = common.cats.jsPromiseToF[F](F)(p)
    def toIO(implicit A: Async[IO]): IO[A] = toF[IO]
  }
}

case class FutureOps[A](val f: Future[A])(implicit ec: ExecutionContext, cs: ContextShift[IO]) {
  def toIO: IO[A] = IO.fromFuture(IO(f))
}

trait FutureSyntax {
  implicit def futureToIO[A](f: Future[A])(implicit ec: ExecutionContext, cs: ContextShift[IO]) =
    FutureOps[A](f)
}

case class IteratorOps[A](val iter: scala.Iterator[A])(implicit ec: ExecutionContext) {

  /** I think this is fs2 `Stream.fromIterator`. */
  def toFS2Stream[A] = Stream.unfold(iter)(i => if (i.hasNext) Some((i.next, i)) else None)
}

trait IteratorSyntax {
  implicit def toIteratorOps[A](iter: scala.Iterator[A])(implicit ec: ExecutionContext) =
    IteratorOps[A](iter)
}
