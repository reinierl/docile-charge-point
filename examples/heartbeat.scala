send(HeartbeatReq)
expectIncoming(matching { case HeartbeatRes(_) => })
