package chargepoint.docile

import java.net.URI
import scala.language.higherKinds

import com.thenewmotion.ocpp.messages.{Message, CentralSystemReqRes, CentralSystemReq, CentralSystemRes}


trait OcppOps[F[_]] {
  def connect(chargerId: String, endpoint: URI, password: Option[String]): F[Unit]

  def disconnect(): F[Unit]

  def send[Q <: CentralSystemReq](req: Q)(implicit reqRes: CentralSystemReqRes[Q, _ <: CentralSystemRes]): F[Unit]

  def expect(): F[Message]
}
