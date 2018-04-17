package chargepoint.docile

import java.net.URI

import chargepoint.docile.test.{RunOnce, Runner, RunnerConfig}
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.S3Event
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.GetObjectRequest
import com.thenewmotion.ocpp.Version

import scala.util.{Failure, Success, Try}

object Lambda extends App {

  import scala.collection.JavaConverters._

  case class Script(name:String, content:Array[Byte])

  def getScriptFromS3(event: S3Event):Option[Script] =
    event.getRecords.asScala.headOption.map(r => {
      val key = r.getS3.getObject.getKey
      val bucket = r.getS3.getBucket.getName
      val s3Client = new AmazonS3Client(new DefaultAWSCredentialsProviderChain)
      val s3Object = s3Client.getObject(new GetObjectRequest(bucket, key))
      val objectData = s3Object.getObjectContent
      val content = Stream.continually(objectData.read).takeWhile(_ != -1).map(_.toByte).toArray
      println(s"found a script called $key in $bucket")
      Script(key, content)
    })

  def executeScript(script:Script): Option[String] = {
    val cpId = sys.env("cpId")
    val cpUri = new URI(sys.env("cpUri"))
    val runnerCfg = RunnerConfig(
      number = 1,
      chargePointId = cpId,
      uri = cpUri,
      ocppVersion = Version.withName(sys.env("cpVersion")).getOrElse(Version.V15),
      authKey = sys.env.get("cpAuthKey"),
      repeat = RunOnce
    )

    println(s"executing ${script.name} as $cpId on $cpUri")
    val runner:Runner = Runner.forBytes(script.name, script.content)


    Try(runner.run(runnerCfg)) match {
      case Success(testsPassed) =>
        val succeeded = Main.summarizeResults(testsPassed)
        sys.exit(if (succeeded) 0 else 1)
      case Failure(e) =>
        System.err.println(s"Could not run tests: ${e.getMessage}")
        e.printStackTrace()
        sys.exit(2)
    }
  }

  def trigger(event: S3Event, context:Context) = {
    println("someone dropped some scala")
    getScriptFromS3(event).flatMap(executeScript)
  }

}
