"connect and send bye" in { ops =>

  for {
    _ <- ops.connect()
    _ <- ops.send(HeartbeatReq)
    _ <- ops.expectIncoming matching { case HeartbeatRes(_) => }
    _ <- ops.disconnect()
  } yield ()
}
