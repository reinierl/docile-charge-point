package chargepoint.docile
package test

import java.io.File
import java.net.URI
import scala.tools.reflect.ToolBox
import scala.util.{Try, Success, Failure}
import akka.actor.ActorSystem
import chargepoint.docile.dsl._
import slogging.StrictLogging
import com.thenewmotion.ocpp

case class RunnerConfig(
  system: ActorSystem,
  chargePointId: String,
  uri: URI,
  ocppVersion: ocpp.Version,
  authKey: Option[String],
  runMode: RunMode
)

class Runner(
  testCases: Seq[Runner.TestCase]
) extends StrictLogging {

  def run(runnerCfg: RunnerConfig): Seq[(String, Either[ScriptFailure, Unit])] =
    runnerCfg.runMode match {
      case OneOff =>
        runOnce(runnerCfg)
      case Repeat(pauseMillis) =>
        runRepeat(runnerCfg, pauseMillis)
    }

  private def runRepeat(runnerCfg: RunnerConfig, pauseMillis: Int): Seq[(String, Either[ScriptFailure, Unit])] = {
    println("Running in repeat mode. Press <ENTER> to stop.")
    var res: Seq[(String, Either[ScriptFailure, Unit])] = Seq.empty

    while (!(System.in.available() > 0)) {
      res = runOnce(runnerCfg)
      Thread.sleep(pauseMillis)
    }

    res
  }

  private def runOnce(runnerCfg: RunnerConfig): Seq[(String, Either[ScriptFailure, Unit])] =
    testCases.map { testCase =>
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

      logger.debug(s"Test running...")

      testCase.name -> res
    }
}

object Runner extends StrictLogging {

  case class TestCase(name: String, test: OcppTest)

  def loadFile(f: String): TestCase = {

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

