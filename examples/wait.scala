"wait for a while" in { ops =>
  for {
    _ <- ops.wait(10.seconds)
  } yield ()
}