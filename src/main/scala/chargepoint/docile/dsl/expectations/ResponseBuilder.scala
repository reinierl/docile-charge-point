package chargepoint.docile
package dsl
package expectations

import com.thenewmotion.ocpp.messages.ChargePointRes

abstract class ResponseBuilder[T] {
  def respondingWith(res: ChargePointRes): T = respondingWith(_ => res)

  def respondingWith(resBuilder: T => ChargePointRes): T
}
