package chargepoint.docile
package test

import scala.util.{Try, Success, Failure}
import slogging.StrictLogging
import dsl.{ReceivedMsgManager, ScriptFailure, ExecutionError}

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
}

