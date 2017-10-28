"submit a transaction" in { ops =>
  for {
    _    <- ops.connect()

    auth <- ops.authorize("12345678").map(_.idTag)

    _    <- if (auth.status == AuthorizationStatus.Accepted) {

      for {
        _       <- ops.statusNotification(status = ChargePointStatus.Occupied(Some(OccupancyKind.Preparing)))

        transId <- ops.startTransaction(meterStart = 300, idTag = "12345678").map(_.transactionId)

        _       <- ops.statusNotification(status = ChargePointStatus.Occupied(Some(OccupancyKind.Charging)))

        _       <- ops.statusNotification(status = ChargePointStatus.Occupied(Some(OccupancyKind.Finishing)))

        _       <- ops.stopTransaction(transactionId = transId, idTag = Some("12345678"))

        _       <- ops.statusNotification(status = ChargePointStatus.Available())
      } yield ()

    } else {
      IntM.pure(())
    }

    _ <- ops.disconnect()
  } yield ()
}

// vim: set ts=4 sw=4 et:
