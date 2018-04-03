package chargepoint.docile
package dsl
package expectations

import com.thenewmotion.ocpp.messages._

trait Ops {
  self: CoreOps =>

  /** An IncomingMessageProcessor[T] is like a PartialFunction[T] with side effects */
  trait IncomingMessageProcessor[+T] {
    def accepts(msg: IncomingMessage): Boolean

    def result(msg: IncomingMessage): T

    def fireSideEffects(msg: IncomingMessage): Unit

    def lift(msg: IncomingMessage): Option[T] =
      if (accepts(msg))
        Some(result(msg))
      else
        None
  }

  sealed trait IncomingRequestProcessor[+T] extends IncomingMessageProcessor[T]

  def expectIncoming[T](proc: IncomingMessageProcessor[T]): T = {
    val promisedMsg = awaitIncoming(1).head

    proc.lift(promisedMsg) match {
      case None =>
        self.fail(s"Expectation failed on ${promisedMsg.message}")
      case Some(t) =>
        proc.fireSideEffects(promisedMsg)
        t
    }
  }

  // TODO: HList time?
  def expectInAnyOrder[T](expectations: IncomingMessageProcessor[T]*): Seq[T] = {
    val messages = awaitIncoming(expectations.length)

    val firstSatisfyingPermutation =
      expectations
        .permutations
        .find(_.zip(messages).forall({case (e, m) => e.accepts(m)}))

    firstSatisfyingPermutation match {
      case None =>
        self.fail(s"Expectation failed on $messages")
      case Some(exps) =>
        exps.zip(messages).map {
          case (e, m) =>
            e.fireSideEffects(m)
            e.result(m)
        }
    }
  }

  def matching[T](matchPF: PartialFunction[Message, T]): IncomingMessageProcessor[T] =
    new IncomingMessageProcessor[T] {
      def accepts(msg: IncomingMessage): Boolean = matchPF.isDefinedAt(msg.message)
      def result(msg: IncomingMessage): T = matchPF.apply(msg.message)
      def fireSideEffects(msg: IncomingMessage): Unit = ()
    }

  def requestMatching[T](
    requestMatch: PartialFunction[ChargePointReq, T]
  ): IncomingRequestProcessor[T] = new IncomingRequestProcessor[T] {
    def accepts(msg: IncomingMessage) = msg match {
      case IncomingRequest(req, _) => requestMatch.isDefinedAt(req)
      case _                       => false
    }

    // TODO nicer protection against cast errors
    def result(msg: IncomingMessage): T = requestMatch.apply(msg.asInstanceOf[IncomingRequest].req)

    def fireSideEffects(msg: IncomingMessage): Unit = ()
  }

  def getConfigurationReq = requestMatching { case r: GetConfigurationReq => r }
  def changeConfigurationReq = requestMatching { case r: ChangeConfigurationReq => r }
  def getDiagnosticsReq = requestMatching { case r: GetDiagnosticsReq => r }
  def changeAvailabilityReq = requestMatching { case r: ChangeAvailabilityReq => r }
  def getLocalListVersionReq = requestMatching { case r if r == GetLocalListVersionReq => r }
  def sendLocalListReq = requestMatching { case r: SendLocalListReq => r }
  def clearCacheReq = requestMatching { case r if r == ClearCacheReq => r }
  def resetReq = requestMatching { case r: ResetReq => r }
  def updateFirmwareReq = requestMatching { case r: UpdateFirmwareReq => r }
  def remoteStartTransactionReq = requestMatching { case r: RemoteStartTransactionReq => r }
  def remoteStopTransactionReq = requestMatching { case r: RemoteStopTransactionReq => r }
  def reserveNowReq = requestMatching { case r: ReserveNowReq => r }
  def cancelReservationReq = requestMatching { case r: CancelReservationReq => r }
  def unlockConnectorReq = requestMatching { case r: UnlockConnectorReq => r }


  implicit class RichIncomingMessageProcessor[T](self: IncomingMessageProcessor[T]) {
    def printingTheMessage: IncomingMessageProcessor[T] = new IncomingMessageProcessor[T] {
      def accepts(msg: IncomingMessage): Boolean = self.accepts(msg)
      def result(msg: IncomingMessage): T = self.result(msg)
      def fireSideEffects(msg: IncomingMessage): Unit = {
        self.fireSideEffects(msg)
        println(msg.message)
      }
    }
  }

  implicit class RichIncomingRequestProcessor[T](self: IncomingRequestProcessor[T]) {
    def respondingWith(res: ChargePointRes): IncomingRequestProcessor[T] = respondingWith(_ => res)

    def respondingWith(resBuilder: T => ChargePointRes): IncomingRequestProcessor[T] = new IncomingRequestProcessor[T] {
      def accepts(msg: IncomingMessage): Boolean = self.accepts(msg)
      def result(msg: IncomingMessage): T = self.result(msg)
      def fireSideEffects(msg: IncomingMessage): Unit = {
        self.fireSideEffects(msg)
        msg match {
          case IncomingRequest(req, respond) =>
            val matchResult = result(msg)
            respond(resBuilder(matchResult))
          case IncomingResponse(incomingRes) =>
            fail(
              "Expecation failed: expected request, " +
                s"received response instead: $incomingRes"
            )
        }
      }
    }
  }

}
