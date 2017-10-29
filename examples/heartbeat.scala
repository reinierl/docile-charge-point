"connect and send bye" in { ops =>

  for {
    _ <- ops.send(HeartbeatReq)
    _ <- ops.expectIncoming matching { case HeartbeatRes(_) => }
  } yield ()
}
