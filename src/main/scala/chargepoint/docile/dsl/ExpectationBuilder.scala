package chargepoint.docile
package dsl

import scala.language.higherKinds
import cats.Monad
import cats.implicits._
import com.thenewmotion.ocpp.messages._

/**
 * Wrapper around a Future[Message] that allows DSL methods to be called on it,
 * like "printingTheMessage" or "matching"
 */
case class ExpectationBuilder[F[_] : Monad](promisedMsg: F[IncomingMessage[F]]) {
  
  def matching[T](matchPF: PartialFunction[Message, T]): F[T] = 
    for (msg <- promisedMsg) yield {
      matchPF.lift(msg.message) match {
        case None =>
          sys.error(s"Expectation failed on $msg")
        case Some(t) =>
          t
      }
    }

  def printingTheMessage: F[Unit] =
    promisedMsg map ((msg: IncomingMessage[F]) => println(msg.message))

  // generalize to any request type
  def getConfigurationReq: ResponseBuilder[F] = new ResponseBuilder[F] {
    def respondingWith(res: ChargePointRes): F[Unit] = {
      for (msg <- promisedMsg) yield {
        msg match {
          case IncomingRequest(msg: GetConfigurationReq, respond) =>
            respond(res)
            ()
          case _ =>
            sys.error(s"Expectation failed on $msg: not GetConfigurationReq")
        }
      }
    }
  }
}
