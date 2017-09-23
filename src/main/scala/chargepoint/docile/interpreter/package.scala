package chargepoint.docile

import cats.implicits._
import cats.data.EitherT

import scala.concurrent.{Future, ExecutionContext}

package object interpreter {
  type IntM[T] = EitherT[Future, ScriptFailure, T]

  object IntM {
    def fromFuture[T](f: Future[T])(implicit ec: ExecutionContext): IntM[T] =
      EitherT.right[ScriptFailure](f)

    def pure[T](t: T)(implicit ec: ExecutionContext): IntM[T] =
      EitherT.pure[Future, ScriptFailure](t)

    def error[T](e: Throwable)(implicit ec: ExecutionContext): IntM[T] =
      EitherT.leftT[Future, T](ExecutionError(e): ScriptFailure)
  }
}
