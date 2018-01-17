package chargepoint.docile.dsl

import chargepoint.docile.dsl.expectations.IncomingMessage
import com.thenewmotion.ocpp.messages.StatusNotificationRes
import org.specs2.mutable.Specification
import org.specs2.concurrent.ExecutionEnv

class ReceivedMsgManagerSpec(implicit ee: ExecutionEnv) extends Specification {

  "ReceivedMsgManager" should {

    "pass on messages to those that requested them" in {
      val sut = new ReceivedMsgManager
      val testMsg = IncomingMessage(StatusNotificationRes)

      val f = sut.dequeue(1)

      f.isCompleted must beFalse

      sut.enqueue(testMsg)

      f must beEqualTo(List(testMsg)).await
    }

    "remember incoming messages until someone dequeues them" in {
      val sut = new ReceivedMsgManager
      val testMsg = IncomingMessage(StatusNotificationRes)

      sut.enqueue(testMsg)

      sut.dequeue(1) must beEqualTo(List(testMsg)).await
    }

    "fulfill request for messages once enough are available" in {
      val sut = new ReceivedMsgManager
      val testMsg = IncomingMessage(StatusNotificationRes)

      sut.enqueue(testMsg)
      sut.enqueue(testMsg)

      val f = sut.dequeue(3)

      f.isCompleted must beFalse

      sut.enqueue(testMsg)

      f must beEqualTo(List(testMsg, testMsg, testMsg)).await
    }
  }
}
