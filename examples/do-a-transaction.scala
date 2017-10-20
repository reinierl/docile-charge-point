"process getdiagnostics request" in { ops =>
  for {
    _    <- ops.connect()
    _    <- ops.send(AuthorizeReq(idTag = "12345678"))
    auth <- ops.expectIncoming matching { case AuthorizeRes(idTagInfo) => idTagInfo }
    _    <- if (auth.status == AuthorizationStatus.Accepted) {
      for {
        _ <- ops.send(StatusNotificationReq(scope = ConnectorScope(0), status = ChargePointStatus.Occupied(Some(OccupancyKind.Preparing)), vendorId = None, timestamp = Some(ZonedDateTime.now())))
        _ <- ops.expectIncoming matching { case StatusNotificationRes => }
        _ <- ops.send(StartTransactionReq(meterStart = 300, idTag = "12345678", timestamp = ZonedDateTime.now(), connector = ConnectorScope(0), reservationId = None))
        transId <- ops.expectIncoming matching { case StartTransactionRes(transactionId, idTagInfo) => transactionId }
        _ <- ops.send(StatusNotificationReq(scope = ConnectorScope(0), status = ChargePointStatus.Occupied(Some(OccupancyKind.Charging)), vendorId = None, timestamp = Some(ZonedDateTime.now())))
        _ <- ops.expectIncoming matching { case StatusNotificationRes => }
        _ <- ops.send(StatusNotificationReq(scope = ConnectorScope(0), status = ChargePointStatus.Occupied(Some(OccupancyKind.Finishing)), vendorId = None, timestamp = Some(ZonedDateTime.now())))
        _ <- ops.expectIncoming matching { case StatusNotificationRes => }
        _ <- ops.send(StopTransactionReq(transactionId = transId, meterStop = 500, idTag = Some("12345678"), timestamp = ZonedDateTime.now(), reason = StopReason.Local, meters = List()))
        _ <- ops.expectIncoming matching { case StopTransactionRes(_) => }
        _ <- ops.send(StatusNotificationReq(scope = ConnectorScope(0), status = ChargePointStatus.Available(), vendorId = None, timestamp = Some(ZonedDateTime.now())))
        _ <- ops.expectIncoming matching { case StatusNotificationRes => }
      } yield ()
    } else {
      IntM.pure(())
    }

    _ <- ops.disconnect()
  } yield ()
}

// vim: set ts=4 sw=4 et:
