val auth = authorize("12345678").idTag

if (auth.status == AuthorizationStatus.Accepted) {

  statusNotification(status = ChargePointStatus.Occupied(Some(OccupancyKind.Preparing)))
  val transId = startTransaction(meterStart = 300, idTag = "12345678").transactionId
  statusNotification(status = ChargePointStatus.Occupied(Some(OccupancyKind.Charging)))

  prompt("Press ENTER to stop charging")

  statusNotification(status = ChargePointStatus.Occupied(Some(OccupancyKind.Finishing)))
  stopTransaction(transactionId = transId, idTag = Some("12345678"))
  statusNotification(status = ChargePointStatus.Available())

} else {
  fail("Not authorized")
}

// vim: set ts=4 sw=4 et:
