"process getdiagnostics request" in { ops =>
  for {
    _ <- ops.connect()
    _ <- ops.expectIncoming matching { case gd: GetDiagnosticsReq => }
    _ <- ops.send(DiagnosticsStatusNotificationReq(DiagnosticsStatus.Uploading))
    _ <- ops.expectIncoming matching { case DiagnosticsStatusNotificationRes => }
    _ <- ops.send(DiagnosticsStatusNotificationReq(DiagnosticsStatus.Uploaded))
    _ <- ops.expectIncoming matching { case DiagnosticsStatusNotificationRes => }
    _ <- ops.disconnect()
  } yield ()
}

// vim: set ts=4 sw=4 et:
