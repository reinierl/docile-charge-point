package chargepoint.docile
package dsl
package shortsend

import java.time.ZonedDateTime

import scala.language.higherKinds
import cats.Monad
import cats.implicits._
import com.thenewmotion.ocpp.messages.{ChargePointStatus, ConnectorScope, StatusNotificationReq, StatusNotificationRes}

trait Ops[F[_]] {

  self: CoreOps[F] with expectations.Ops[F] =>

  implicit val m: Monad[F]

  def statusNotification(
    scope: ConnectorScope = ConnectorScope(0),
    status: ChargePointStatus = ChargePointStatus.Available(),
    timestamp: Option[ZonedDateTime] = Some(ZonedDateTime.now()),
    vendorId: Option[String] = None
  ): F[StatusNotificationRes.type] =
    for {
      _ <- self.send(StatusNotificationReq(scope = scope, status = status, timestamp = timestamp, vendorId = vendorId))
      res <- self.expectIncoming matching { case res: StatusNotificationRes.type => res }
    } yield res
}
