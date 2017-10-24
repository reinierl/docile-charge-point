package chargepoint.docile
package dsl

import cats.Monad
import cats.implicits._

import scala.language.higherKinds

trait ExtraOps[F[_]] {

  self: CoreOps[F] =>

  implicit val m: Monad[F]

  def expectIncoming: ExpectationBuilder[F] =
    // TODO use implicit to pass them core ops?
    new ExpectationBuilder[F](
      awaitIncoming(1).map(_.head)
    ) {
      val core: CoreOps[F] = self
    }
}

