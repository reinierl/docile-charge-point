package chargepoint.docile

import java.net.URI

import scala.util.{Failure, Success, Try}
import chargepoint.docile.dsl.{ExecutionError, ExpectationFailed}
import com.thenewmotion.ocpp.Version
import org.rogach.scallop._
import slogging.{LogLevel, LoggerConfig, PrintLoggerFactory, StrictLogging}
import test._

object Main extends App with StrictLogging {

  object conf extends ScallopConf(args) {
    implicit val versionConverter =
      singleArgConverter(
        Version.withName(_).get, {
          case _: NoSuchElementException => Left("Invalid OCPP version provided")
        }
      )

    val version = opt[Version](
      default = Some(Version.V16),
      descr = "OCPP version"
    )

    val authKey = opt[String](
      descr = "Authorization key to use for Basic Auth (hex-encoded, 40 characters)"
    )

    val chargePointId = opt[String](
      default = Some("03000001"),
      descr = "ChargePointIdentity to identify ourselves to the Central System"
    )

    val interactive = toggle(
      default = Some(false),
      descrYes = "Start REPL to enter and run a test interactively"
    )

    val numberInParallel = opt[Int](
      default = None,
      descr = "Start given number of instances of the script at the same time (can be combined with --repeat)"
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
      descr = "Verbosity (0-5)"
    )

    val uri = trailArg[URI](
      descr = "URI of the Central System"
    )

    val files = trailArg[List[String]](
      required = false,
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

  implicit val ec = concurrent.ExecutionContext.Implicits.global


  conf.numberInParallel.toOption match {
    case None | Some(1) =>
      singleShotRun()
    case Some(n) if n < 1 =>
      println("Tssk, grapjas.")
      sys.exit(1)
    case Some(n) =>
      parallelRun(conf.numberInParallel())
  }

  def singleShotRun(): Unit = {

    val runnerCfg = runnerConfigWithChargePointId(conf.chargePointId())

    val runner: Runner =
      if (conf.interactive())
        Runner.interactive
      else
        filesRunner()

    Try(runner.run(runnerCfg)) match {
      case Success(testsPassed) =>
        val succeeded = summarizeResults(testsPassed)
        sys.exit(if (succeeded) 0 else 1)
      case Failure(e) =>
        System.err.println(s"Could not run tests: ${e.getMessage}")
        e.printStackTrace()
        sys.exit(2)
    }
  }

  private def summarizeResults(testResults: Seq[(String, TestResult)]): Boolean = {

    val outcomes = testResults map { case (testName, outcome) =>

      val outcomeDescription = outcome match {
        case TestFailed(ExpectationFailed(msg)) => s"❌  $msg"
        case TestFailed(ExecutionError(e)) => s"💥  ${e.getClass.getSimpleName} ${e.getMessage}"
        case TestPassed => s"✅"
      }

      println(s"$testName: $outcomeDescription")

      outcome
    }

    logger.debug("Finished testing, returning from runner")

    outcomes.collect({ case TestFailed(_) => }).isEmpty
  }

  // TODO we'll probably want to push parallel running deeper down into runnerland, because:
  //  * aggregate reporting
  //  * sane way to handle interaction (stopping the repeat, prompts)
  //  * keeping connection open during repeat runs
  def parallelRun(n: Int): Unit = {
    val runner = filesRunner()

    1.to(n) foreach { i =>
      val runnerConfig = runnerConfigWithChargePointId(conf.chargePointId().format(i))

      new Thread {
        override def run(): Unit = runner.run(runnerConfig)
      }.start()
    }
  }

  private def filesRunner(): Runner = {
    val files = conf.files.getOrElse {
      sys.error(
        "You have to give files on the command-line for a non-interactive run"
      )
    }
    Runner.forFiles(files)
  }

  private def runnerConfigWithChargePointId(cpId: String): RunnerConfig =
    RunnerConfig(
      chargePointId = cpId,
      uri = conf.uri(),
      ocppVersion = conf.version(),
      authKey = conf.authKey.toOption,
      repeat =
        if (conf.repeat())
          Repeat(conf.repeatPause())
        else
          RunOnce
    )
}
