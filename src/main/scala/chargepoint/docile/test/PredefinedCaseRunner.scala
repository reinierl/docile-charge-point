package chargepoint.docile
package test

import scala.util.{Failure, Success, Try}
import slogging.StrictLogging
import dsl.{ExecutionError, ReceivedMsgManager, ScriptFailure}

class PredefinedCaseRunner(testCases: Seq[TestCase]) extends Runner with StrictLogging {

  def run(runnerCfg: RunnerConfig): Seq[(String, TestResult)] = runnerCfg.repeat match {
    case RunOnce       => runOnce(testCases, runnerCfg)
    case Repeat(pause) => runRepeat(testCases, runnerCfg, pauseMillis = pause)
  }

  private def runRepeat(testCases: Seq[TestCase], runnerCfg: RunnerConfig, pauseMillis: Int): Seq[(String, TestResult)] = {
    println("Running in repeat mode. Press <ENTER> to stop.")
    var res: Seq[(String, TestResult)] = Seq.empty

    while (!(System.in.available() > 0)) {
      res = runOnce(testCases, runnerCfg)
      Thread.sleep(pauseMillis)
    }

    res
  }

  private def runOnce(testCases: Seq[TestCase], runnerCfg: RunnerConfig): Seq[(String, TestResult)] =
    testCases map { testCase =>
      runCase(runnerCfg, testCase)
    }

  private def runCase(runnerCfg: RunnerConfig, c: TestCase): (String, TestResult) = {
    logger.debug(s"Going to connect ${c.name}")
    c.test.connect(
      new ReceivedMsgManager(),
      runnerCfg.chargePointId,
      runnerCfg.uri,
      runnerCfg.ocppVersion,
      runnerCfg.authKey
    )

    logger.info(s"Going to run ${c.name}")

    val res = Try(c.test.run()) match {
      case Success(_)                => TestPassed
      case Failure(e: ScriptFailure) => TestFailed(e)
      case Failure(e: Exception)     => TestFailed(ExecutionError(e))
      case Failure(e)                => throw e
    }

    logger.debug(s"Test ${c.name} run; disconnecting...")

    c.test.disconnect()

    logger.debug(s"Disconnected OCPP connection for ${c.name}")

    c.name -> res
  }
}

