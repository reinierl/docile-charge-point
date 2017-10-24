"submit a transaction" in { ops =>
  for {
    _    <- ops.connect()

    _    <- ops.send(AuthorizeReq(idTag = "12345678"))
    auth <- ops.expectIncoming matching { case AuthorizeRes(idTagInfo) => idTagInfo }

    _    <- if (auth.status == AuthorizationStatus.Accepted) {

      for {
        _ <- ops.statusNotification(status = ChargePointStatus.Occupied(Some(OccupancyKind.Preparing)))

        _ <- ops.send(StartTransactionReq(meterStart = 300, idTag = "12345678", timestamp = ZonedDateTime.now(), connector = ConnectorScope(0), reservationId = None))
        transId <- ops.expectIncoming matching { case StartTransactionRes(transactionId, idTagInfo) => transactionId }

        _ <- ops.statusNotification(status = ChargePointStatus.Occupied(Some(OccupancyKind.Charging)))

        _ <- ops.statusNotification(status = ChargePointStatus.Occupied(Some(OccupancyKind.Finishing)))

        _ <- ops.send(StopTransactionReq(transactionId = transId, meterStop = 500, idTag = Some("12345678"), timestamp = ZonedDateTime.now(), reason = StopReason.Local, meters = List()))
        _ <- ops.expectIncoming matching { case StopTransactionRes(_) => }

        _ <- ops.statusNotification(status = ChargePointStatus.Available())
      } yield ()

    } else {
      IntM.pure(())
    }

    _ <- ops.disconnect()
  } yield ()
}

// vim: set ts=4 sw=4 et:
