package chargepoint.docile
package interpreter

import java.net.URI

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import cats.implicits._
import com.thenewmotion.ocpp.json.{OcppError, OcppJsonClient}
import com.thenewmotion.ocpp.messages._

import scala.concurrent.{Future, ExecutionContext, Promise}
import scala.concurrent.duration._
import scala.util.{Success, Failure}
import dsl.{IncomingMessage, ExpectationBuilder, CoreOps}

class Ocpp15JInterpreter(system: ActorSystem) extends CoreOps[Future] {

  implicit val ec: ExecutionContext = system.dispatcher

  implicit val expectTimeout: Timeout = Timeout(5.seconds)

  var connection: Option[OcppJsonClient] = None

  val receivedMsgs = system.actorOf(ReceivedMsgManager.props())

  def connect(chargerId: String, endpoint: URI, password: Option[String]): Future[Unit] = {
    connection = Some {
      new OcppJsonClient(chargerId, endpoint, password) {
        override def onDisconnect(): Unit = {
          System.out.println(s"Disconnection confirmed by OCPP library")
          connection = null
        }

        override def onError(e: OcppError): Unit = {
          System.err.println(s"Received error: $e")
        }

        override def onRequest(req: ChargePointReq): Future[ChargePointRes] = {
          System.out.println(s"Received incoming request: $req")

          val responsePromise = Promise[ChargePointRes]()

          def respond(res: ChargePointRes): Future[Unit] = Future.successful {
            responsePromise.success(res)
          }.mapTo[Unit]

          receivedMsgs ! ReceivedMsgManager.Enqueue(
            IncomingMessage[Future](req, respond _)
          )

          responsePromise.future
        }
      }
    }

    Future.successful(())
  }

  def disconnect() = Future.successful(connection.foreach(_.close()))

  def send[Q <: CentralSystemReq](req: Q)(implicit reqRes: CentralSystemReqRes[Q, _ <: CentralSystemRes]): Future[Unit] = connection match {
    case None =>
      System.err.println ("Trying to send req while not connected")
      Future.successful (() )
    case Some (client) => Future.successful {
        client.send(req)(reqRes) onComplete {
          case Success(res) =>
            receivedMsgs ! ReceivedMsgManager.Enqueue(
              IncomingMessage[Future](res)
            )
          case Failure(e) => 
            System.err.println(s"Something went wrong sending OCPP request $req: ${e.getMessage}")
            e.printStackTrace()
        }
      }
  }

  def expectIncoming: ExpectationBuilder[Future] =
    ExpectationBuilder(
      (receivedMsgs ? ReceivedMsgManager.Dequeue).mapTo[IncomingMessage[Future]]
    )
}
