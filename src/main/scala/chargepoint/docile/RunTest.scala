package chargepoint.docile

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

import akka.actor.ActorSystem
import cats.instances.future._

import interpreter.Ocpp15JInterpreter

object RunTest extends App {

  val system = ActorSystem()

  implicit val ec = system.dispatcher

  val interpreter = new Ocpp15JInterpreter(system)

  System.out.println("interpreter instantiated")

  Await.result(TestScript.connectAndSendBootAndBye[Future](interpreter), 5.seconds)
}
