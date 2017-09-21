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

  def requestMatching(
    requestMatch: PartialFunction[ChargePointReq, Unit]
  ): ResponseBuilder[F] = new ResponseBuilder[F] {

    def respondingWith(res: ChargePointRes): F[Unit] = {
      for (msg <- promisedMsg) yield {
        msg match {
          case IncomingRequest(msg: GetConfigurationReq, respond) =>
            if (requestMatch.isDefinedAt(msg)) {
              respond(res)
              ()
            } else {
              sys.error(s"Expectation failed on $msg: not GetConfigurationReq")
            }
          case IncomingResponse(incomingRes) =>
            sys.error(
              "Expecation failed: expected request, " +
              s"received response instead: $incomingRes"
            )
        }
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
