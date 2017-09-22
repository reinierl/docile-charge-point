package chargepoint.docile
package interpreter

import scala.collection.mutable
import akka.actor.{ActorRef, Actor, Props}
import dsl.IncomingMessage

class ReceivedMsgManager extends Actor {

  import ReceivedMsgManager._

  val messages = mutable.Queue[IncomingMessage[IntM]]()

  val waiters = mutable.Queue[ActorRef]()

  def receive = {
    case Enqueue(msg) =>
      messages.enqueue(msg)

      if (waiters.nonEmpty) deliverOne()

    case Dequeue =>
      waiters.enqueue(sender())

      if (messages.nonEmpty) deliverOne()
  }

  private def deliverOne(): Unit = {
    waiters.dequeue() ! messages.dequeue()
  }
}

object ReceivedMsgManager {
  def props(): Props = Props[ReceivedMsgManager]()

  sealed trait Command
  case class Enqueue(msg: IncomingMessage[IntM]) extends Command
  case object Dequeue extends Command
}
