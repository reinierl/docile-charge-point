val chargeTokenId = "01234567"
val auth = authorize(chargeTokenId).idTag

if (auth.status == AuthorizationStatus.Accepted) {

  statusNotification(status = ChargePointStatus.Occupied(Some(OccupancyKind.Preparing)))
  val transId = startTransaction(meterStart = 300, idTag = chargeTokenId).transactionId
  statusNotification(status = ChargePointStatus.Occupied(Some(OccupancyKind.Charging)))

  prompt("Press ENTER to stop charging")

  statusNotification(status = ChargePointStatus.Occupied(Some(OccupancyKind.Finishing)))
  stopTransaction(transactionId = transId, idTag = Some(chargeTokenId))
  statusNotification(status = ChargePointStatus.Available())

} else {
  fail("Not authorized")
}

// vim: set ts=4 sw=4 et:
