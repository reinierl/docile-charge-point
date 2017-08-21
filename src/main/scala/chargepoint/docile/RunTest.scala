package chargepoint.docile

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

import scala.concurrent.ExecutionContext.Implicits.global
import cats.instances.future._

import interpreter.Ocpp15JInterpreter

object RunTest extends App {

  val interpreter = new Ocpp15JInterpreter()

  System.out.println("interpreter instantiated")

  Await.result(TestScript.connectAndSendBootAndBye[Future](interpreter), 5.seconds)
}