package chargepoint.docile.interpreter

sealed trait ScriptFailure
case class ExpectationFailed(message: String) extends ScriptFailure
case class ExecutionError(e: Throwable) extends ScriptFailure
