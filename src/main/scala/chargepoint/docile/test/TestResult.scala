package chargepoint.docile
package test

import dsl.ScriptFailure

sealed trait TestResult
case object TestPassed extends TestResult
case class TestFailed(failure: ScriptFailure) extends TestResult
