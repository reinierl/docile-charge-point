package chargepoint.docile
package test

import dsl.{CoreOps, OcppTest}
import dsl.{expectations, shortsend}

class InteractiveOcppTest extends OcppTest {
  val ops = new CoreOps with expectations.Ops with shortsend.Ops {
    def ocppConnection = InteractiveOcppTest.this.ocppConnection
    def receivedMsgManager = InteractiveOcppTest.this.receivedMsgManager
  }

  def run(): Unit = {
    val imports =
      """
        |import ops._
        |import scala.language.postfixOps
        |import scala.concurrent.duration._
        |import java.time._
        |import com.thenewmotion.ocpp.messages._
      """.stripMargin
    ammonite.Main(predefCode = imports).run("ops" -> ops)
  }
}
