package chargepoint.docile

import scala.concurrent.Await
import scala.concurrent.duration._
import java.net.URI
import akka.actor.ActorSystem
import cats.instances.future._
import com.thenewmotion.ocpp.Version
import org.rogach.scallop._

import interpreter.{Ocpp15JInterpreter, ExpectationFailed, ExecutionError}
import cats.Monad

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

    verify()
  }

  val system = ActorSystem()

  implicit val ec = system.dispatcher

  val testRunResults = {
    val testableObject = new TestScript[interpreter.IntM] {
      protected val m = implicitly[Monad[interpreter.IntM]]
      System.out.println("Interpreter instantiated")
    }

    val int = new Ocpp15JInterpreter(
      system,
      conf.chargePointId(),
      conf.uri(),
      conf.version(),
      conf.authKey.toOption
    )

    testableObject.tests.toList.map { test =>
      test.title -> test.program(int).value
    }
  }

  testRunResults foreach { testResult =>
    val res = Await.result(testResult._2, 5.seconds)

    val outcomeDescription = res match {
      case Left(ExpectationFailed(msg)) => s"❌  $msg"
      case Left(ExecutionError(e))      => s"💥  ${e.getClass.getSimpleName} ${e.getMessage}"
      case Right(())                     => s"✅"
    }

    System.out.println(s"${testResult._1}: $outcomeDescription")
  }

  system.terminate()
}
