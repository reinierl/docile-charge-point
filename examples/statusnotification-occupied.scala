"mark charge point as occupied" in { ops =>

  for {
    _ <- ops.connect()

    _ <- ops.send(StatusNotificationReq(scope = ConnectorScope(0), timestamp = Some(ZonedDateTime.now()), status = ChargePointStatus.Occupied(None), vendorId = None))
    _ <- ops.expectIncoming matching { case StatusNotificationRes => }

    _ <- ops.disconnect()
  } yield ()
}

// vim: set ts=4 sw=4 et:
