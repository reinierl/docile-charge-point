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

  def requestMatching[T](
    requestMatch: PartialFunction[ChargePointReq, T]
  ): ResponseBuilder[T] = new ResponseBuilder[T] {

    def respondingWith(resBuilder: T => ChargePointRes): T = {
        promisedMsg match {
          case IncomingRequest(msg, respond) =>
            if (requestMatch.isDefinedAt(msg)) {
              val matchResult = requestMatch.apply(msg)
              respond(resBuilder(matchResult))
              matchResult
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

  def getConfigurationReq = requestMatching { case r: GetConfigurationReq => r }
  def changeConfigurationReq = requestMatching { case r: ChangeConfigurationReq => r }
  def getDiagnosticsReq = requestMatching { case r: GetDiagnosticsReq => r }
  def changeAvailabilityReq = requestMatching { case r: ChangeAvailabilityReq => r }
  def getLocalListVersionReq = requestMatching { case r if r == GetLocalListVersionReq => r }
  def sendLocalListReq = requestMatching { case r: SendLocalListReq => r }
  def clearCacheReq = requestMatching { case r if r == ClearCacheReq => r }
  def resetReq = requestMatching { case r: ResetReq => r }
  def updateFirmwareReq = requestMatching { case r: UpdateFirmwareReq => r }
  def remoteStartTransactionReq = requestMatching { case r: RemoteStartTransactionReq => r }
  def remoteStopTransactionReq = requestMatching { case r: RemoteStopTransactionReq => r }
  def reserveNowReq = requestMatching { case r: ReserveNowReq => r }
  def cancelReservationReq = requestMatching { case r: CancelReservationReq => r }
  def unlockConnectorReq = requestMatching { case r: UnlockConnectorReq => r }
}
