package chargepoint.docile.test

sealed trait RunMode
case object OneOff extends RunMode
case class Repeat(pause: Int) extends RunMode
