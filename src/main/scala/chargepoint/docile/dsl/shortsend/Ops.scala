package chargepoint.docile
package dsl
package shortsend

import java.time.ZonedDateTime

import scala.language.higherKinds
import cats.Monad
import cats.implicits._
import com.thenewmotion.ocpp.messages._

import scala.reflect.ClassTag

trait Ops[F[_]] {

  self: CoreOps[F] with expectations.Ops[F] =>

  implicit val m: Monad[F]

  def authorize(idTag: String): F[AuthorizeRes] =
    sendSync(AuthorizeReq(idTag))

  def startTransaction(
    connector: ConnectorScope = ConnectorScope(0),
    idTag: String = "ABCDEF01",
    timestamp: ZonedDateTime = ZonedDateTime.now,
    meterStart: Int = 0,
    reservationId: Option[Int] = None
  ): F[StartTransactionRes] =
    sendSync(StartTransactionReq(
      connector,
      idTag,
      timestamp,
      meterStart,
      reservationId
    ))

  def statusNotification(
    scope: ConnectorScope = ConnectorScope(0),
    status: ChargePointStatus = ChargePointStatus.Available(),
    timestamp: Option[ZonedDateTime] = Some(ZonedDateTime.now()),
    vendorId: Option[String] = None
  ): F[StatusNotificationRes.type] =
    sendSync(StatusNotificationReq(
      scope,
      status,
      timestamp,
      vendorId
    ))

  def stopTransaction(
    transactionId: Int = 0,
    idTag: Option[String] = Some("ABCDEF01"),
    timestamp: ZonedDateTime = ZonedDateTime.now,
    meterStop: Int = 16000,
    reason: StopReason = StopReason.Local,
    meters: List[meter.Meter] = List()
  ): F[StopTransactionRes] =
    sendSync(StopTransactionReq(
      transactionId,
      idTag,
      timestamp,
      meterStop,
      reason,
      meters
    ))

  def sendSync[REQ <: CentralSystemReq, RES <: CentralSystemRes : ClassTag](req: REQ)(implicit reqRes: CentralSystemReqRes[REQ, RES]): F[RES] =
    for {
      _ <- self.send(req)
      res <- self.expectIncoming matching { case res: RES => res }
    } yield res
}
