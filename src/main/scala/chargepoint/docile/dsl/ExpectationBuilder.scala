package chargepoint.docile
package dsl

import scala.language.higherKinds

import cats.Monad
import cats.implicits._
import com.thenewmotion.ocpp.messages.Message

/**
 * Wrapper around a Future[Message] that allows DSL methods to be called on it,
 * like "printingTheMessage" or "matching"
 */
case class ExpectationBuilder[F[_] : Monad](promisedMsg: F[Message]) {
  
  def matching[T](matchPF: PartialFunction[Message, T]): F[T] = 
    for (msg <- promisedMsg) yield {
      matchPF.lift(msg) match {
        case None =>
          sys.error(s"Expectation failed on $msg")
        case Some(t) =>
          t
      }
    }

  def printingTheMessage: F[Unit] = promisedMsg map ((msg: Message) => println(msg))
}
