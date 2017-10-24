package chargepoint.docile
package dsl
package expectations

import scala.language.higherKinds

import cats.Monad
import cats.implicits._
import com.thenewmotion.ocpp.messages._

/**
 * Wrapper around a Future[Message] that allows DSL methods to be called on it,
 * like "printingTheMessage" or "matching"
 */
abstract class ExpectationBuilder[F[_] : Monad](promisedMsg: F[IncomingMessage[F]]) {

  def core: CoreOps[F]
  
  def matching[T](matchPF: PartialFunction[Message, T]): F[T] = 
    for {
      msg <- promisedMsg
      matchRes <- matchPF.lift(msg.message) match {
        case None =>
          core.typedFailure[T](s"Expectation failed on $msg")
        case Some(t) =>
          t.pure[F]
      }
    } yield matchRes

  def printingTheMessage: F[Unit] =
    promisedMsg map ((msg: IncomingMessage[F]) => println(msg.message))

  def requestMatching(
    requestMatch: PartialFunction[ChargePointReq, Unit]
  ): ResponseBuilder[F] = new ResponseBuilder[F] {

    def respondingWith(res: ChargePointRes): F[Unit] = {
      for {
        msg <- promisedMsg
        res <- msg match {
          case IncomingRequest(msg, respond) =>
            if (requestMatch.isDefinedAt(msg)) {
              respond(res)
              ().pure[F]
            } else {
              core.fail(s"Expectation failed on $msg: not GetConfigurationReq")
            }
          case IncomingResponse(incomingRes) =>
           core.fail(
              "Expecation failed: expected request, " +
              s"received response instead: $incomingRes"
            )
        }
      } yield res
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
