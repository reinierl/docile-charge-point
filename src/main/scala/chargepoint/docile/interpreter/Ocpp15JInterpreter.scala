package chargepoint.docile
package interpreter

import java.net.URI

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import cats.data.EitherT
import cats.implicits._
import com.thenewmotion.ocpp.Version
import com.thenewmotion.ocpp.json.api.{ChargePointRequestHandler, OcppError,
                                      OcppJsonClient}
import com.thenewmotion.ocpp.messages._
import slogging.StrictLogging

import scala.concurrent.{ExecutionContext, Promise}
import scala.concurrent.duration._
import scala.util.{Success, Failure}
import dsl.{IncomingMessage, ExpectationBuilder, CoreOps}

class Ocpp15JInterpreter(
  system: ActorSystem,
  chargerId: String,
  endpoint: URI,
  version: Version,
  authKey: Option[String]
) extends CoreOps[IntM] with StrictLogging {

  implicit val ec: ExecutionContext = system.dispatcher

  implicit val expectTimeout: Timeout = Timeout(45.seconds)

  var connection: Option[OcppJsonClient] = None

  val receivedMsgs = system.actorOf(ReceivedMsgManager.props())

  def connect(): IntM[Unit] = {

    connection = Some {
      new OcppJsonClient(chargerId, endpoint, version, authKey) {
        override def onDisconnect(): Unit = {
          logger.debug(s"Disconnection confirmed by OCPP library")
          connection = null
        }

        override def onError(e: OcppError): Unit = {
          logger.info(s"Received OCPP error: $e")
        }

        override def requestHandler: ChargePointRequestHandler = {
          (req: ChargePointReq) =>
            logger.info(s"<< $req")

            val responsePromise = Promise[ChargePointRes]()

            def respond(res: ChargePointRes): IntM[Unit] = IntM.pure {
              logger.info(s">> $res")
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
        logger.info(s">> $req")
        client.send(req)(reqRes) onComplete {
          case Success(res) =>
            logger.info(s"<< $res")
            receivedMsgs ! ReceivedMsgManager.Enqueue(
              IncomingMessage[IntM](res)
            )
          case Failure(e) =>
            logger.debug("Failed to get response to outgoing OCPP request")
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
