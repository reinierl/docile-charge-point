package chargepoint.docile
package test

import scala.util.{Try, Success, Failure}
import slogging.StrictLogging
import dsl.{ReceivedMsgManager, ScriptFailure, ExecutionError}

class PredefinedCaseRunner(testCases: Seq[TestCase]) extends Runner with StrictLogging {

  def run(runnerCfg: RunnerConfig): Seq[(String, Either[ScriptFailure, Unit])] = runnerCfg.repeat match {
    case RunOnce       => runOnce(testCases, runnerCfg)
    case Repeat(pause) => runRepeat(testCases, runnerCfg, pauseMillis = pause)
  }

  private def runRepeat(testCases: Seq[TestCase], runnerCfg: RunnerConfig, pauseMillis: Int): Seq[(String, Either[ScriptFailure, Unit])] = {
    println("Running in repeat mode. Press <ENTER> to stop.")
    var res: Seq[(String, Either[ScriptFailure, Unit])] = Seq.empty

    while (!(System.in.available() > 0)) {
      res = runOnce(testCases, runnerCfg)
      Thread.sleep(pauseMillis)
    }

    res
  }

  private def runOnce(testCases: Seq[TestCase], runnerCfg: RunnerConfig): Seq[(String, Either[ScriptFailure, Unit])] =
    testCases map { testCase =>
      logger.debug(s"Going to connect ${testCase.name}")
      testCase.test.connect(
        runnerCfg.system.actorOf(ReceivedMsgManager.props()),
        runnerCfg.chargePointId,
        runnerCfg.uri,
        runnerCfg.ocppVersion,
        runnerCfg.authKey
      )

      logger.info(s"Going to run ${testCase.name}")

      val res = Try(testCase.test.run()) match {
        case Success(_)                => Right(())
        case Failure(e: ScriptFailure) => Left(e)
        case Failure(e: Exception)     => Left(ExecutionError(e))
        case Failure(e)                => throw e
      }

      logger.debug(s"Test ${testCase.name} run; disconnecting...")

      testCase.test.disconnect()

      logger.debug(s"Disconnected OCPP connection for ${testCase.name}")

      testCase.name -> res
    }
}

