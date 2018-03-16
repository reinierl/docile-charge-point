package chargepoint.docile.test

sealed trait RepeatMode
case object RunOnce extends RepeatMode
// TODO split in indefinite repeat (like ~ in sbt) and limited repeat for use with --number
case class Repeat(pause: Int) extends RepeatMode
