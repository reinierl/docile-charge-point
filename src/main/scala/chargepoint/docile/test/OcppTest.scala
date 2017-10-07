package chargepoint.docile
package test

import scala.language.higherKinds
import scala.collection.mutable
import cats.Monad
import dsl.CoreOps

trait OcppTest[F[_]] {

  protected implicit val m: Monad[F]

  case class TestCase(title: String, program: CoreOps[F] => F[Unit])

  var tests: mutable.ArrayBuffer[TestCase] = mutable.ArrayBuffer()

  implicit class StringAsTestTitle(title: String) {
    def in(program: CoreOps[F] => F[Unit]): Unit = {
      tests += TestCase(title, program)
      ()
    }
  }
}

