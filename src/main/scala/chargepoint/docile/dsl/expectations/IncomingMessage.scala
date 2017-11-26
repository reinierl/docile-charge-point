package chargepoint.docile
package dsl
package expectations

import com.thenewmotion.ocpp.messages._

abstract class IncomingMessage {
  def message: Message
}

object IncomingMessage {
  def apply(res: CentralSystemRes) = IncomingResponse(res)

  def apply(
    req: ChargePointReq,
    respond: ChargePointRes => Unit
  ) = IncomingRequest(req, respond)
}

case class IncomingResponse(
  res: CentralSystemRes
) extends IncomingMessage {
  def message: CentralSystemRes = res
}

case class IncomingRequest(
  req: ChargePointReq,
  respond: ChargePointRes => Unit
) extends IncomingMessage {
  def message: ChargePointReq = req
}
