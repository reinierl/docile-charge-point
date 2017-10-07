package chargepoint.docile

import scala.concurrent.Await
import scala.concurrent.duration._

import akka.actor.ActorSystem
import cats.instances.future._

import interpreter.{Ocpp15JInterpreter, ExpectationFailed, ExecutionError}
import cats.Monad

object RunTest extends App {

  val system = ActorSystem()

  implicit val ec = system.dispatcher

  val testRunResults = {
    val testableObject = new TestScript[interpreter.IntM] {
      protected val m = implicitly[Monad[interpreter.IntM]]
      System.out.println("Interpreter instantiated")
    }

    testableObject.tests.toList.map { test =>
      test.title -> test.program(new Ocpp15JInterpreter(system)).value
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
