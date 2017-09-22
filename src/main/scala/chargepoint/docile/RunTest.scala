package chargepoint.docile

import scala.concurrent.Await
import scala.concurrent.duration._

import akka.actor.ActorSystem
import cats.instances.future._

import interpreter.Ocpp15JInterpreter

object RunTest extends App {

  val system = ActorSystem()

  implicit val ec = system.dispatcher

  val interpreter = new Ocpp15JInterpreter(system)

  System.out.println("interpreter instantiated")

  val res = Await.result(TestScript.connectAndSendBootAndBye(interpreter).value, 5.seconds)

  System.out.println(s"Test result: $res")

  system.terminate()
}
