package chargepoint.docile
package dsl
package expectations

import cats.Monad
import com.thenewmotion.ocpp.messages.ChargePointRes

import scala.language.higherKinds

abstract class ResponseBuilder[F[_]: Monad] {
  def respondingWith(res: ChargePointRes): F[Unit]
}
