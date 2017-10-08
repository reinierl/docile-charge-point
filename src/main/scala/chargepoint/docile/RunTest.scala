package chargepoint.docile

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.tools.reflect.ToolBox
import java.net.URI
import java.io.File
import akka.actor.ActorSystem
import com.thenewmotion.ocpp.Version
import org.rogach.scallop._

import interpreter.{Ocpp15JInterpreter, ExpectationFailed, ExecutionError}
import test.OcppTest

object RunTest extends App {

  object conf extends ScallopConf(args) {
    implicit val versionConverter =
      singleArgConverter(Version.withName(_).get, {
        case _: NoSuchElementException => Left("Invalid OCPP version provided")
      })

    val version = opt[Version](
      default = Some(Version.V16),
      descr = "OCPP version"
    )

    val authKey = opt[String](
      descr = "Authorization key to use for Basic Auth (hex-encoded, 40 characters)"
    )

    val chargePointId = opt[String](
      default = Some("03000001"),
      descr="ChargePointIdentity to identify ourselves to the Central System"
    )

    val uri = trailArg[URI](
      descr = "URI of the Central System"
    )

    val files = trailArg[List[String]](
      descr = "files with test cases to load"
    )

    verify()
  }

  val system = ActorSystem()

  implicit val ec = system.dispatcher

  val testCases = conf.files().map { f =>
    val file = new File(f)

    import reflect.runtime.currentMirror
    val toolbox = currentMirror.mkToolBox()

    val preamble = s"""
                    |import scala.language.{higherKinds, postfixOps}
                    |import scala.concurrent.ExecutionContext.Implicits.global
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
    val appendix = "}"

    val fileContents = scala.io.Source.fromFile(file).getLines.mkString("\n")

    val fileAst = toolbox.parse(preamble + fileContents + appendix)

    System.err.println(s"Parsed $f")

    val compiledCode = toolbox.compile(fileAst)

    System.err.println(s"Compiled $f")

    compiledCode().asInstanceOf[OcppTest[interpreter.IntM]]
  }

  val testRunResults = {

    testCases.flatMap(_.tests.toList).map { test =>
      System.err.println(s"Instantiating interpreter for ${test.title}")

      val int = new Ocpp15JInterpreter(
        system,
        conf.chargePointId(),
        conf.uri(),
        conf.version(),
        conf.authKey.toOption
      )

      System.err.println(s"Going to run ${test.title}")

      val res = test.title -> test.program(int).value
      System.err.println(s"Test running...")
      res
    }
  }

  val outcomes = testRunResults map { testResult =>
    System.err.println(s"Awaiting test ${testResult._1}")
    val res = Await.result(testResult._2, 5.seconds)

    val outcomeDescription = res match {
      case Left(ExpectationFailed(msg)) => s"❌  $msg"
      case Left(ExecutionError(e))      => s"💥  ${e.getClass.getSimpleName} ${e.getMessage}"
      case Right(())                     => s"✅"
    }

    System.out.println(s"${testResult._1}: $outcomeDescription")

    res
  }

  System.out.println("End of main body reached, terminating Akka...")
  system.terminate() foreach { _ =>
    System.exit(if (!outcomes.exists(_.isLeft)) 0 else 1)
  }
}
