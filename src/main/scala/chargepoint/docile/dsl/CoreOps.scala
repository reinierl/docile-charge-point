package chargepoint.docile
package dsl

import java.net.URI
import scala.language.higherKinds

import com.thenewmotion.ocpp.messages.{CentralSystemReqRes, CentralSystemReq, CentralSystemRes}


trait CoreOps[F[_]] {
  def connect(chargerId: String, endpoint: URI, password: Option[String]): F[Unit]

  def disconnect(): F[Unit]

  def send[Q <: CentralSystemReq](req: Q)(implicit reqRes: CentralSystemReqRes[Q, _ <: CentralSystemRes]): F[Unit]

  def expectIncoming: ExpectationBuilder[F]

  def fail(message: String): F[Unit] = typedFailure[Unit](message)

  def typedFailure[T](message: String): F[T]
}
