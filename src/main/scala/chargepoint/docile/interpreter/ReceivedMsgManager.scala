package chargepoint.docile
package interpreter

import scala.collection.mutable
import akka.actor.{ActorRef, Actor, Props}
import dsl.IncomingMessage

class ReceivedMsgManager extends Actor {

  import ReceivedMsgManager._

  private val messages = mutable.Queue[IncomingMessage[IntM]]()

  private val waiters = mutable.Queue[Waiter]()

  def receive = {
    case Enqueue(msg) =>
      System.err.println(s"Enqueueing $msg")
      messages += msg
      tryToDeliver()

    case Dequeue(numMsgs) =>
      System.err.println(s"Trying to dequeue $numMsgs")
      waiters += Waiter(sender(), numMsgs)
      tryToDeliver()
  }

  private def tryToDeliver(): Unit = {
    if (readyToDequeue) {
      System.err.println("dequeuing...")
      val waiter = waiters.dequeue

      val delivery = mutable.ArrayBuffer[IncomingMessage[IntM]]()

      1.to(waiter.numberOfMessages) foreach { _ =>
        delivery += messages.dequeue()
      }

      System.err.println(s"tryToDeliver delivering ${delivery.toList}")
      waiter.requester ! delivery.toList
    } else {
      System.err.println("Not ready to deliver")
    }
  }

  private def readyToDequeue: Boolean =
    waiters.headOption map (_.numberOfMessages) exists (_ <= messages.size)
}

object ReceivedMsgManager {
  def props(): Props = Props[ReceivedMsgManager]()

  sealed trait Command
  case class Enqueue(msg: IncomingMessage[IntM]) extends Command
  case class Dequeue(numMsgs: Int = 1) extends Command

  private case class Waiter(requester: ActorRef, numberOfMessages: Int)
}
