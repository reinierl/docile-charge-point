package chargepoint.docile.dsl

import java.net.URI
import scala.concurrent.Promise
import scala.concurrent.ExecutionContext.Implicits.global
import chargepoint.docile.dsl.expectations.IncomingMessage
import com.thenewmotion.ocpp.Version
import com.thenewmotion.ocpp.json.api._
import com.thenewmotion.ocpp.messages.{ChargePointReq, ChargePointRes}
import slogging.StrictLogging

trait OcppTest extends StrictLogging {
  protected var connectionData: OcppConnectionData = _

  def runConnected(
    receivedMsgManager: ReceivedMsgManager,
    chargerId: String,
    endpoint: URI,
    version: Version,
    authKey: Option[String]
  ): Unit = {
    connect(receivedMsgManager, chargerId, endpoint, version, authKey)
    run()
    disconnect()
  }

  private def connect(
    receivedMsgManager: ReceivedMsgManager,
    chargerId: String,
    endpoint: URI,
    version: Version,
    authKey: Option[String]
  ): Unit = {
    val connection = new OcppJsonClient(chargerId, endpoint, List(version), authKey) {

      override def onDisconnect(): Unit = {
        logger.debug(s"Disconnection confirmed by OCPP library")
        connectionData = connectionData.copy(ocppClient = None)
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

          receivedMsgManager.enqueue(
            IncomingMessage(req, respond)
          )

          responsePromise.future
      }
    }

    connectionData = OcppConnectionData(Some(connection), receivedMsgManager, chargerId)
  }

  private def disconnect(): Unit = connectionData.ocppClient.foreach(_.close())

  protected def run(): Unit
}

case class OcppConnectionData(
  /**
    * The current OCPP connection
    *
    * This is a mutable Option[OcppJsonClient] instead of an immutable
    * OcppJsonClient because I hope this will allow us to write tests that
    * disconnect and reconnect when we have a more complete test DSL.
    */
  ocppClient: Option[OcppJsonClient],
  receivedMsgManager: ReceivedMsgManager,
  chargePointIdentity: String
)
