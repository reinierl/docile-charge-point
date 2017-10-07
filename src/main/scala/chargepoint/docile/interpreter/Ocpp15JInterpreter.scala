package chargepoint.docile
package interpreter

import java.net.URI

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import cats.data.EitherT
import cats.implicits._
import com.thenewmotion.ocpp.json.{OcppError, OcppJsonClient}
import com.thenewmotion.ocpp.messages._

import scala.concurrent.{Future, ExecutionContext, Promise}
import scala.concurrent.duration._
import scala.util.{Success, Failure}
import dsl.{IncomingMessage, ExpectationBuilder, CoreOps}

class Ocpp15JInterpreter(system: ActorSystem) extends CoreOps[IntM] {

  implicit val ec: ExecutionContext = system.dispatcher

  implicit val expectTimeout: Timeout = Timeout(5.seconds)

  var connection: Option[OcppJsonClient] = None

  val receivedMsgs = system.actorOf(ReceivedMsgManager.props())

  def connect(chargerId: String, endpoint: URI, password: Option[String]): IntM[Unit] = {
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

          def respond(res: ChargePointRes): IntM[Unit] = IntM.pure {
            responsePromise.success(res)
            ()
          }

          receivedMsgs ! ReceivedMsgManager.Enqueue(
            IncomingMessage[IntM](req, respond _)
          )

          responsePromise.future
        }
      }
    }

    IntM.pure(())
  }

  def disconnect() = IntM.pure(connection.foreach(_.close()))

  def send[Q <: CentralSystemReq](req: Q)(implicit reqRes: CentralSystemReqRes[Q, _ <: CentralSystemRes]): IntM[Unit] = connection match {
    case None =>
      IntM.error[Unit](
        new Exception("Trying to send an OCPP message while not connected")
      )
    case Some (client) => IntM.pure {
        client.send(req)(reqRes) onComplete {
          case Success(res) =>
            receivedMsgs ! ReceivedMsgManager.Enqueue(
              IncomingMessage[IntM](res)
            )
          case Failure(e) =>
            IntM.error[Unit](e)
        }
      }
  }

  def expectIncoming: ExpectationBuilder[IntM] =
    new ExpectationBuilder[IntM](
      IntM.fromFuture {
        (receivedMsgs ? ReceivedMsgManager.Dequeue()).mapTo[List[IncomingMessage[IntM]]]
      }.map(_.head)
    ) {
      override val core: CoreOps[IntM] = Ocpp15JInterpreter.this
    }

  def typedFailure[T](message: String): IntM[T] =
    EitherT.leftT(ExpectationFailed(message))
}
