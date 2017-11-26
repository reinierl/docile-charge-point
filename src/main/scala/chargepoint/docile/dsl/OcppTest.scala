package chargepoint.docile.dsl

import java.net.URI

import scala.concurrent.Promise
import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.ActorRef
import chargepoint.docile.dsl.expectations.IncomingMessage
import com.thenewmotion.ocpp.Version
import com.thenewmotion.ocpp.json.api._
import com.thenewmotion.ocpp.messages.{ChargePointReq, ChargePointRes}
import slogging.StrictLogging

abstract class OcppTest extends StrictLogging {
  /**
    * The current OCPP connection
    *
    * This is a mutable Option[OcppJsonClient] instead of an immutable
    * OcppJsonClient because I hope this will allow us to write tests that
    * disconnect and reconnect when we have a more complete test DSL.
    */
  var ocppConnection: Option[OcppJsonClient] = None

  var receivedMsgManager: ActorRef = _

  def connect(
    receivedMsgManager: ActorRef,
    chargerId: String,
    endpoint: URI,
    version: Version,
    authKey: Option[String]
  ): Unit = {
    this.receivedMsgManager = receivedMsgManager
    ocppConnection = Some{
      new OcppJsonClient(chargerId, endpoint, List(version), authKey) {

        override def onDisconnect(): Unit = {
          logger.debug(s"Disconnection confirmed by OCPP library")
          ocppConnection = None
        }

        override def onError(e: OcppError): Unit = {
          logger.info(s"Received OCPP error: $e")
        }

        override def requestHandler: ChargePointRequestHandler = {
          (req: ChargePointReq) =>
            logger.info(s"<< $req")

            val responsePromise = Promise[ChargePointRes]()

            def respond(res: ChargePointRes): Unit = {
              logger.info(s">> $res")
              responsePromise.success(res)
              ()
            }

            receivedMsgManager ! ReceivedMsgManager.Enqueue(
              IncomingMessage(req, respond)
            )

            responsePromise.future
        }
      }
    }
  }

  def disconnect(): Unit = ocppConnection.foreach(_.close())

  def run(): Unit
}
