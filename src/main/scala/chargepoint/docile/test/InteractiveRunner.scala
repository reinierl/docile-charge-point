package chargepoint.docile
package test

import dsl.{CoreOps, OcppTest}
import dsl.{expectations, shortsend}

// TODO Do we really need this? Or can we instantiate the interactiveTestCase and pass it into the PredefinedCaseRunner in the Runner factory method?
class InteractiveRunner extends Runner {

  def run(cfg: RunnerConfig): Seq[(String, TestResult)] =
    Seq(runCase(cfg, interactiveTestCase))

  def interactiveTestCase: TestCase = TestCase("Interactive test",
    new OcppTest with CoreOps with expectations.Ops with shortsend.Ops {

      def run(): Unit = {
        ammonite.Main().run()
      }
    }
  )
}
