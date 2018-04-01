package chargepoint.docile.test

sealed trait RepeatMode

case object RunOnce extends RepeatMode

sealed trait RunRepeated extends RepeatMode {
  def pause: Int
}

case class Repeat(numberOfTimes: Int, pause: Int) extends RunRepeated
case class UntilSuccess(pause: Int)               extends RunRepeated
case class Indefinitely(pause: Int)               extends RunRepeated
