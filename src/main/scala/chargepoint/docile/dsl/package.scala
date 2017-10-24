package chargepoint.docile

import scala.language.higherKinds

package object dsl {
  type FullDslOps[F[_]] = CoreOps[F] with expectations.Ops[F] with shortsend.Ops[F]
}
