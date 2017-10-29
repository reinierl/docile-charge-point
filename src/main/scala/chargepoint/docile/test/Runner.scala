package chargepoint.docile
package test

import java.io.File
import java.net.URI
import scala.tools.reflect.ToolBox
import scala.concurrent.{ExecutionContext, Future}
import akka.actor.ActorSystem
import cats.Monad
import cats.implicits._
import slogging.StrictLogging
import com.thenewmotion.ocpp

import interpreter.{IntM, OcppJInterpreter, ScriptFailure}

case class RunnerConfig(
  system: ActorSystem,
  chargePointId: String,
  uri: URI,
  ocppVersion: ocpp.Version,
  authKey: Option[String]
)

class Runner(
  cfg: RunnerConfig,
  testCases: Seq[OcppTest[IntM]]
) extends StrictLogging {
  def run(): Seq[(String, Future[Either[ScriptFailure, Unit]])] =
    testCases.flatMap(_.tests.toList).map { test =>
      logger.debug(s"Instantiating interpreter for ${test.title}")

      implicit val ec: ExecutionContext = cfg.system.dispatcher

      val int = new OcppJInterpreter(
        cfg.system,
        cfg.chargePointId,
        cfg.uri,
        cfg.ocppVersion,
        cfg.authKey
      ) with dsl.expectations.Ops[IntM] with dsl.shortsend.Ops[IntM] {
        val m: Monad[IntM] = implicitly[Monad[IntM]]
      }

      logger.info(s"Going to run ${test.title}")

      val res = for {
        _ <- int.connect()
        testResult <- test.program(int)
        _ <- int.disconnect()
      } yield testResult

      logger.debug(s"Test running...")

      test.title -> res.value
    }
}

object Runner extends StrictLogging {

  def loadFile(f: String): OcppTest[IntM] = {

    val file = new File(f)

    import reflect.runtime.currentMirror
    val toolbox = currentMirror.mkToolBox()

    val preamble = s"""
                   |import scala.language.{higherKinds, postfixOps}
                   |import scala.concurrent.ExecutionContext.Implicits.global
                   |import java.time._
                   |import cats.implicits._
                   |import cats.instances._
                   |import cats.syntax._
                   |import cats.Monad
                   |import com.thenewmotion.ocpp.messages._
                   |import chargepoint.docile.interpreter.IntM
                   |import chargepoint.docile.test.OcppTest
                   |
                   |new OcppTest[IntM] {
                   |  val m = implicitly[Monad[IntM]];
                   """.stripMargin
    val appendix = "\n}"

    val fileContents = scala.io.Source.fromFile(file).getLines.mkString("\n")

    val fileAst = toolbox.parse(preamble + fileContents + appendix)

    logger.debug(s"Parsed $f")

    val compiledCode = toolbox.compile(fileAst)

    logger.debug(s"Compiled $f")

    compiledCode().asInstanceOf[OcppTest[IntM]]
  }
}

