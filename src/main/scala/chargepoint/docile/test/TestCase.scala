package chargepoint.docile
package test

import dsl.OcppTest

/** The result of loading a script file: test name and a factory to create a
  * runnable OcppTest instance.
  */
case class TestCase(name: String, test: () => OcppTest)

