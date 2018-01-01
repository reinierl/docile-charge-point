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
  repeat: RepeatMode
)

trait Runner extends StrictLogging {

  def run(runnerCfg: RunnerConfig): Seq[(String, TestResult)]

  protected def runCase(runnerCfg: RunnerConfig, c: TestCase): (String, TestResult) = {
    logger.debug(s"Going to connect ${c.name}")
    c.test.connect(
      runnerCfg.system.actorOf(ReceivedMsgManager.props()),
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

  def interactive(config: RunnerConfig): Runner = new InteractiveRunner()

  def forFiles(files: Seq[String], config: RunnerConfig): Runner =
    new PredefinedCaseRunner(files.map(loadFile))

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

