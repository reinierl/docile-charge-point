package chargepoint.docile.dsl

import scala.language.higherKinds

import cats.Monad
import com.thenewmotion.ocpp.messages._

abstract class IncomingMessage[F[_]: Monad] {
  def message: Message
}

object IncomingMessage {
  def apply[F[_]: Monad](res: CentralSystemRes) = IncomingResponse[F](res)

  def apply[F[_]: Monad](
    req: ChargePointReq,
    respond: ChargePointRes => F[Unit]
  ) = IncomingRequest(req, respond)
}

case class IncomingResponse[F[_]: Monad](
  res: CentralSystemRes
) extends IncomingMessage[F] {
  def message: CentralSystemRes = res
}

case class IncomingRequest[F[_]: Monad](
  req: ChargePointReq,
  respond: ChargePointRes => F[Unit]
) extends IncomingMessage[F] {
  def message: ChargePointReq = req
}
