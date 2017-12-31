package chargepoint.docile
package test

import java.io.File
import java.net.URI
import scala.tools.reflect.ToolBox
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

trait Runner {
  def run(runnerCfg: RunnerConfig): Seq[(String, Either[ScriptFailure, Unit])]
}

object Runner extends StrictLogging {

  def interactive(config: RunnerConfig): Runner = ???

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

