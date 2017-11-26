package chargepoint.docile
package dsl
package expectations

import com.thenewmotion.ocpp.messages.ChargePointRes

abstract class ResponseBuilder {
  def respondingWith(res: ChargePointRes): Unit
}
