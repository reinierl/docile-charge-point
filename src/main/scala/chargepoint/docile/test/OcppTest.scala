package chargepoint.docile
package test

import scala.language.higherKinds
import scala.collection.mutable
import cats.Monad
import dsl.CoreOps

/**
 * Extend this to create your OCPP test case.
 *
 * You define test cases like this:
 *
 *    "get response to heartbeat" in { ops =>
 *      for {
 *        _ <- ops.connect()
 *        _ <- ops.send(HeartbeatReq)
 *        _ <- ops.expectIncoming.matching { case HeartbeatRes(_) => }
 *        _ <- ops.disconnect()
 *      } yield ()
 */
trait OcppTest[F[_]] {

  implicit protected val m: Monad[F]

  case class TestCase(title: String, program: CoreOps[F] => F[Unit])

  var tests: mutable.ArrayBuffer[TestCase] = mutable.ArrayBuffer()

  implicit class StringAsTestTitle(title: String) {
    def in(program: CoreOps[F] => F[Unit]): Unit = {
      tests += TestCase(title, program)
      ()
    }
  }
}

