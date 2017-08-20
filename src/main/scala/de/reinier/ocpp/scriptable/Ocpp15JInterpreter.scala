package de.reinier.ocpp.scriptable

import java.net.URI

import com.thenewmotion.ocpp.json.{OcppError, OcppJsonClient}
import com.thenewmotion.ocpp.messages.{ChargePointRes, ChargePointReq, CentralSystemReq, CentralSystemRes,  CentralSystemReqRes}

import scala.collection.mutable
import scala.concurrent.{Future, ExecutionContext}

class Ocpp15JInterpreter(implicit ec: ExecutionContext) extends OcppOps[Future] {

  var connection: Option[OcppJsonClient] = None

  // TODO: make generic in CS/CP
  val receivedMsgs = mutable.Queue[ChargePointReq]()

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
          receivedMsgs.enqueue(req)
          throw new RuntimeException("not implemented!")
        }
      }
    }

    Future.successful(())
  }

  // TODO asynchronous close in OCPP lib?
  def disconnect() = Future.successful(connection.foreach(_.close()))

  def send[Q <: CentralSystemReq](req: Q)(implicit reqRes: CentralSystemReqRes[Q, _ <: CentralSystemRes]): Future[Unit] = connection match {
      // TODO: form of error reporting / tracking
    case None =>
      System.err.println ("Trying to send req while not connected")
      Future.successful (() )
    case Some (client) =>
      Future(client.send(req)(reqRes)) map (_ => ())
  }

  def expect() = ???
}
