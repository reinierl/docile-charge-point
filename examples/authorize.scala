"authorize Reinier's charge token" in { ops =>
  for {
    _ <- ops.connect()
    _ <- ops.send(AuthorizeReq(idTag = "ABCDEF01020304"))
    _ <- ops.expectIncoming matching { case AuthorizeRes(idTi) if idTi.status == AuthorizationStatus.Accepted => }
    _ <- ops.disconnect()
  } yield ()
}
