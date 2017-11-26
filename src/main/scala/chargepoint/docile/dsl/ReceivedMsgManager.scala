package chargepoint.docile.dsl

import akka.actor.{Actor, ActorRef, Props}
import chargepoint.docile.dsl.expectations.IncomingMessage
import slogging.StrictLogging

import scala.collection.mutable

class ReceivedMsgManager extends Actor with StrictLogging {

  import ReceivedMsgManager._

  private val messages = mutable.Queue[IncomingMessage]()

  private val waiters = mutable.Queue[Waiter]()

  def receive = {
    case Enqueue(msg) =>
      logger.debug(s"Enqueueing $msg")
      messages += msg
      tryToDeliver()

    case Dequeue(numMsgs) =>
      logger.debug(s"Trying to dequeue $numMsgs")
      waiters += Waiter(sender(), numMsgs)
      tryToDeliver()
  }

  private def tryToDeliver(): Unit = {
    if (readyToDequeue) {
      logger.debug("delivering queued messages to expecters...")
      val waiter = waiters.dequeue

      val delivery = mutable.ArrayBuffer[IncomingMessage]()

      1.to(waiter.numberOfMessages) foreach { _ =>
        delivery += messages.dequeue()
      }

      logger.debug(s"delivering ${delivery.toList}")
      waiter.requester ! delivery.toList
    } else {
      logger.debug("Not ready to deliver")
    }
  }

  private def readyToDequeue: Boolean =
    waiters.headOption map (_.numberOfMessages) exists (_ <= messages.size)
}

object ReceivedMsgManager {
  def props(): Props = Props[ReceivedMsgManager]()

  sealed trait Command
  case class Enqueue(msg: IncomingMessage) extends Command
  case class Dequeue(numMsgs: Int = 1) extends Command

  private case class Waiter(requester: ActorRef, numberOfMessages: Int)
}
