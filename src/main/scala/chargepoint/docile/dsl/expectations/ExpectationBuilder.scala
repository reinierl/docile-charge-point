package chargepoint.docile
package dsl
package expectations

import com.thenewmotion.ocpp.messages._

/**
 * Wrapper around a Future[Message] that allows DSL methods to be called on it,
 * like "printingTheMessage" or "matching"
 */
abstract class ExpectationBuilder(promisedMsg: IncomingMessage) {

  def core: CoreOps

  def matching[T](matchPF: PartialFunction[Message, T]): T =
    matchPF.lift(promisedMsg.message) match {
      case None =>
        core.fail(s"Expectation failed on ${promisedMsg.message}")
      case Some(t) =>
        t
    }

  def printingTheMessage: Unit = println(promisedMsg.message)

  def requestMatching(
    requestMatch: PartialFunction[ChargePointReq, Unit]
  ): ResponseBuilder = new ResponseBuilder {

    def respondingWith(res: ChargePointRes): Unit = {
        promisedMsg match {
          case IncomingRequest(msg, respond) =>
            if (requestMatch.isDefinedAt(msg)) {
              respond(res)
              ()
            } else {
              core.fail(s"Expectation failed on $msg: not GetConfigurationReq")
            }
          case IncomingResponse(incomingRes) =>
           core.fail(
              "Expecation failed: expected request, " +
              s"received response instead: $incomingRes"
            )
        }
    }
  }

  def getConfigurationReq = requestMatching { case _: GetConfigurationReq => }
  def changeConfigurationReq = requestMatching { case _: ChangeConfigurationReq => }
  def getDiagnosticsReq = requestMatching { case _: GetDiagnosticsReq => }
  def changeAvailabilityReq = requestMatching { case _: ChangeAvailabilityReq => }
  def getLocalListVersionReq = requestMatching { case GetLocalListVersionReq => }
  def sendLocalListReq = requestMatching { case _: SendLocalListReq => }
  def clearCacheReq = requestMatching { case ClearCacheReq => }
  def resetReq = requestMatching { case _: ResetReq => }
  def updateFirmwareReq = requestMatching { case _: UpdateFirmwareReq => }
  def remoteStartTransactionReq = requestMatching { case _: RemoteStartTransactionReq => }
  def remoteStopTransactionReq = requestMatching { case _: RemoteStopTransactionReq => }
  def reserveNowReq = requestMatching { case _: ReserveNowReq => }
  def cancelReservationReq = requestMatching { case _: CancelReservationReq => }
  def unlockConnectorReq = requestMatching { case _: UnlockConnectorReq => }
}
