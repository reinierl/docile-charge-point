package chargepoint.docile.test

sealed trait RepeatMode
case object RunOnce extends RepeatMode
case class Repeat(pause: Int) extends RepeatMode
