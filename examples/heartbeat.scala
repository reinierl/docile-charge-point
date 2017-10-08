"connect and send bye" in { ops =>

  for {
    _ <- ops.connect()
    _ <- {System.err.println("hoi"); ops.send(HeartbeatReq)}
    _ <- {System.err.println("Going to aawait that res"); ops.expectIncoming matching { case HeartbeatRes(_) => } }
    _ <- {System.err.println("received heartbeat"); ops.disconnect()}
  } yield ()
}
