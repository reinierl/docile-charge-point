package chargepoint.docile
package interpreter

import akka.actor.{Actor, Props, ActorRef}
import com.thenewmotion.ocpp.messages.Message

import scala.collection.mutable

class ReceivedMsgManager extends Actor {

  import ReceivedMsgManager._

  val messages = mutable.Queue[Message]()

  val waiters = mutable.Queue[ActorRef]()

  def receive = {
    case Enqueue(msg) =>
      messages.enqueue(msg)

      if (!waiters.isEmpty) deliverOne()

    case Dequeue =>
      waiters.enqueue(sender())

      if (!messages.isEmpty) deliverOne()
  }

  private def deliverOne(): Unit = {
    waiters.dequeue() ! messages.dequeue()
  }
}

object ReceivedMsgManager {
    def props(): Props = Props[ReceivedMsgManager]()

    sealed trait Command
    case class Enqueue(msg: Message) extends Command
    case object Dequeue extends Command
}
