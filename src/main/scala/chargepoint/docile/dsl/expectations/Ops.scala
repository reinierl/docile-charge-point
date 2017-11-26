package chargepoint.docile
package dsl
package expectations

trait Ops {
  self: CoreOps =>

  def expectIncoming: ExpectationBuilder =
    new ExpectationBuilder(
      awaitIncoming(1).head
    ) {
      val core: CoreOps = self
    }
}
