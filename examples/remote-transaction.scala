import com.thenewmotion.ocpp.messages._

println("Waiting for remote start message")
val startRequest = expectIncoming.remoteStartTransactionReq.respondingWith(RemoteStartTransactionRes(true))
val chargeTokenId = startRequest.idTag

println("Received remote start, authorizing...")
val auth = authorize(chargeTokenId).idTag

if (auth.status == AuthorizationStatus.Accepted) {
  println("Obtained authorization from Central System; starting transaction")

  statusNotification(status = ChargePointStatus.Occupied(Some(OccupancyKind.Preparing)))
  val transId = startTransaction(meterStart = 300, idTag = chargeTokenId).transactionId
  statusNotification(status = ChargePointStatus.Occupied(Some(OccupancyKind.Charging)))

  println(s"Transaction started with ID $transId; awaiting remote stop")

  def waitForValidRemoteStop(): Unit = {
    val stopReq = expectIncoming.remoteStopTransactionReq.respondingWith({ (req: RemoteStopTransactionReq) =>
      RemoteStopTransactionRes {
        // TODO duplicate check - we need a better way
        req.transactionId == transId
      }
    })

    if (stopReq.transactionId == transId) {
      println("Received RemoteStopTransaction request; stopping transaction")
      ()
    } else {
      println(s"Received RemoteStopTransaction request for other transaction with ID ${stopReq.transactionId}. I'll keep waiting for a stop for $transId.")
      waitForValidRemoteStop()
    }
  }

  waitForValidRemoteStop()

  statusNotification(status = ChargePointStatus.Occupied(Some(OccupancyKind.Finishing)))
  stopTransaction(transactionId = transId, idTag = Some(chargeTokenId))
  statusNotification(status = ChargePointStatus.Available())

  println("Transaction stopped")

} else {
  println("Authorization denied by Central System")
  fail("Not authorized")
}

// vim: set ts=4 sw=4 et:
