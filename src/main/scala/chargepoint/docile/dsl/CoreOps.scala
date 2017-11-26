package chargepoint.docile
package dsl

import java.util.concurrent.TimeoutException

import scala.concurrent.duration._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}
import akka.pattern.ask
import akka.util.Timeout
import com.thenewmotion.ocpp.messages.{CentralSystemReq, CentralSystemReqRes, CentralSystemRes}
import expectations.IncomingMessage
import slogging.StrictLogging

trait CoreOps extends StrictLogging {

  self: OcppTest  =>

  def send[Q <: CentralSystemReq](req: Q)(implicit reqRes: CentralSystemReqRes[Q, _ <: CentralSystemRes]): Unit =
    ocppConnection match {
      case None =>
        throw ExpectationFailed("Trying to send an OCPP message while not connected")
      case Some (client) =>
        logger.info(s">> $req")
        client.send(req)(reqRes) onComplete {
          case Success(res) =>
            logger.info(s"<< $res")
            receivedMsgManager ! ReceivedMsgManager.Enqueue(
              IncomingMessage(res)
            )
          case Failure(e) =>
            logger.debug("Failed to get response to outgoing OCPP request")
            // TODO handle this nicer; should be possible to write scripts expecting failure (without using catch ;-) )
            throw ExecutionError(e)
    }
  }


  def awaitIncoming(num: Int): Seq[IncomingMessage] = {
    implicit val askTimeout = Timeout(45.seconds)
    def getMsgs = (receivedMsgManager ? ReceivedMsgManager.Dequeue(num)).mapTo[List[IncomingMessage]]
    Try(Await.result(getMsgs, 47.seconds)) match {
      case Success(msgs)                => msgs
      case Failure(e: TimeoutException) => fail(s"Expected message not received after 45 seconds")
      case Failure(e)                   => error(e)
    }
  }

  def fail(message: String): Nothing = throw ExpectationFailed(message)

  def error(throwable: Throwable): Nothing = throw ExecutionError(throwable)

  def wait(duration: FiniteDuration): Unit = Thread.sleep(duration.toMillis)

  def prompt(cue: String): String = {
    println(s"$cue: ")
    scala.io.StdIn.readLine()
  }
}
