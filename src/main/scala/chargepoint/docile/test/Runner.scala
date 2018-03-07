package chargepoint.docile
package test

import java.io.File
import java.net.URI
import scala.tools.reflect.ToolBox
import scala.util.{Try, Success, Failure}
import chargepoint.docile.dsl._
import slogging.StrictLogging
import com.thenewmotion.ocpp

case class RunnerConfig(
  chargePointId: String,
  uri: URI,
  ocppVersion: ocpp.Version,
  authKey: Option[String],
  repeat: RepeatMode
)

/**
  * A runner runs a sequence of given test cases according to a certain configuration
  *
  * @param testCases The test cases to run
  */
class Runner(testCases: Seq[TestCase]) extends StrictLogging {

  // How to do parallelism here...
  //   - match first on the parallel n
  //   - If multiple: call runParallel
  //   runParallel will manage:
  //     - starting multiple threads, that each:
  //       - call runOnce from different threads
  //       - print completion note??? Or return a Stream of those?
  //           -> yeah let's return a Stream, also from the run() altogether
  //     - watching for termination command
  //     - use old school interrupt / join to wait for termination
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


object Runner extends StrictLogging {

  def interactive: Runner = new Runner(
    Seq(TestCase("Interactive test", new InteractiveOcppTest))
  )

  def forFiles(files: Seq[String]): Runner =
    new Runner(files.map(loadFile))

  private def loadFile(f: String): TestCase = {

    val file = new File(f)
    val testNameRegex = "(?:.*/)?([^/]+?)(?:\\.[^.]*)?$".r
    val testName = f match {
      case testNameRegex(n) => n
      case _                => f
    }

    import reflect.runtime.currentMirror
    val toolbox = currentMirror.mkToolBox()

    val preamble = s"""
                   |import scala.language.postfixOps
                   |import scala.concurrent.duration._
                   |import java.time._
                   |import com.thenewmotion.ocpp.messages._
                   |
                   |new chargepoint.docile.dsl.OcppTest
                   |  with chargepoint.docile.dsl.CoreOps
                   |  with chargepoint.docile.dsl.expectations.Ops
                   |  with chargepoint.docile.dsl.shortsend.Ops {
                   |
                   |  def run() {
                   """.stripMargin
    val appendix = ";\n  }\n}"

    val fileContents = scala.io.Source.fromFile(file).getLines.mkString("\n")

    val fileAst = toolbox.parse(preamble + fileContents + appendix)

    logger.debug(s"Parsed $f")

    val compiledCode = toolbox.compile(fileAst)

    logger.debug(s"Compiled $f")

    TestCase(testName, compiledCode().asInstanceOf[OcppTest])
  }
}

