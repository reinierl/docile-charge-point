package chargepoint.docile.dsl

import scala.language.higherKinds

import cats.Monad
import com.thenewmotion.ocpp.messages.ChargePointRes

abstract class ResponseBuilder[F[_]: Monad] {
  def respondingWith(res: ChargePointRes): F[Unit]
}
