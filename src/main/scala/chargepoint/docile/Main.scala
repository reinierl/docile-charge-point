package chargepoint.docile

import java.net.URI

import scala.util.{Failure, Success, Try}
import akka.actor.ActorSystem
import chargepoint.docile.dsl.{ExecutionError, ExpectationFailed, ScriptFailure}
import com.thenewmotion.ocpp.Version
import org.rogach.scallop._
import slogging.{LogLevel, LoggerConfig, PrintLoggerFactory, StrictLogging}
import test._

object Main extends App with StrictLogging {

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

    val interactive = toggle(
      default = Some(false),
      descrYes = "Start REPL to enter and run a test interactively"
    )

    val repeat = toggle(
      default = Some(false),
      descrYes = "Keep executing script until terminated"
    )

    val repeatPause = opt[Int](
      default = Some(1000),
      descr = "Number of milliseconds to wait between repeat runs"
    )

    val verbose = opt[Int](
      default = Some(3),
      descr="Verbosity (0-5)"
    )

    val uri = trailArg[URI](
      descr = "URI of the Central System"
    )

    val files = trailArg[List[String]](
      descr = "files with test cases to load"
    )

    verify()
  }

  LoggerConfig.factory = PrintLoggerFactory()
  LoggerConfig.level = conf.verbose() match {
    case 0 => LogLevel.OFF
    case 1 => LogLevel.ERROR
    case 2 => LogLevel.WARN
    case 3 => LogLevel.INFO
    case 4 => LogLevel.DEBUG
    case 5 => LogLevel.TRACE
    case _ => sys.error("Invalid verbosity, should be 0, 1, 2, 3, 4 or 5")
  }

  val system = ActorSystem()

  implicit val ec = system.dispatcher

  val runnerCfg = RunnerConfig(
    system = system,
    chargePointId = conf.chargePointId(),
    uri = conf.uri(),
    ocppVersion = conf.version(),
    authKey = conf.authKey.toOption,
    repeat =
      if (conf.repeat())
        Repeat(conf.repeatPause())
      else
        RunOnce
  )

  val runner: Runner =
    if (conf.interactive())
      Runner.interactive(runnerCfg)
    else
      Runner.forFiles(conf.files(), runnerCfg)

  Try(runner.run(runnerCfg)) match {
    case Success(testsPassed) =>
      val succeeded = summarizeResults(testsPassed)
      forceExit(succeeded)
    case Failure(e) =>
      System.err.println(s"Could not run tests: ${e.getMessage}")
      e.printStackTrace()
      forceExit(false)
  }

  private def summarizeResults(testResults: Seq[(String, Either[ScriptFailure, Unit])]): Boolean = {

    val outcomes = testResults map { case (testName, outcome) =>

      val outcomeDescription = outcome match {
        case Left(ExpectationFailed(msg)) => s"❌  $msg"
        case Left(ExecutionError(e))      => s"💥  ${e.getClass.getSimpleName} ${e.getMessage}"
        case Right(())                     => s"✅"
      }

      println(s"$testName: $outcomeDescription")

      outcome
    }

    logger.debug("Finished testing, returning from runner")

    !outcomes.exists(_.isLeft)
  }



  private def forceExit(success: Boolean): Unit = {
    system.terminate() onComplete { _ =>
      sys.exit(if (success) 0 else 1)
    }
  }
}
