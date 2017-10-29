"authorize Reinier's charge token" in { ops =>
  for {
    _ <- ops.send(AuthorizeReq(idTag = "ABCDEF01020304"))
    _ <- ops.expectIncoming matching { case AuthorizeRes(idTi) if idTi.status == AuthorizationStatus.Accepted => }
  } yield ()
}
