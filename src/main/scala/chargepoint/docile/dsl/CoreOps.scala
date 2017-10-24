package chargepoint.docile
package dsl

import scala.language.higherKinds
import com.thenewmotion.ocpp.messages.{CentralSystemReq, CentralSystemReqRes, CentralSystemRes}

import expectations.IncomingMessage

trait CoreOps[F[_]] {
  def connect(): F[Unit]

  def disconnect(): F[Unit]

  def send[Q <: CentralSystemReq](req: Q)(implicit reqRes: CentralSystemReqRes[Q, _ <: CentralSystemRes]): F[Unit]

  def awaitIncoming(num: Int): F[Seq[IncomingMessage[F]]]

  def fail(message: String): F[Unit] = typedFailure[Unit](message)

  def typedFailure[T](message: String): F[T]
}
