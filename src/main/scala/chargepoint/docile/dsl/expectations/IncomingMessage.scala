package chargepoint.docile
package dsl
package expectations

import com.thenewmotion.ocpp.messages._
import com.thenewmotion.ocpp.json.api.OcppError

sealed trait IncomingMessage

object IncomingMessage {
  def apply(res: CentralSystemRes) = IncomingResponse(res)

  def apply(
    req: ChargePointReq,
    respond: ChargePointRes => Unit
  ) = IncomingRequest(req, respond)

  def apply(error: OcppError) = IncomingError(error)
}

case class IncomingResponse(
  res: CentralSystemRes
) extends IncomingMessage

case class IncomingRequest(
  req: ChargePointReq,
  respond: ChargePointRes => Unit
) extends IncomingMessage

case class IncomingError(
  error: OcppError
) extends IncomingMessage
