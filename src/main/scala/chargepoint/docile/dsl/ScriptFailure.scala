package chargepoint.docile.dsl

sealed trait ScriptFailure extends RuntimeException
case class ExpectationFailed(message: String) extends ScriptFailure
case class ExecutionError(e: Throwable) extends ScriptFailure
