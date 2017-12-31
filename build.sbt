enablePlugins(OssLibPlugin)

scalaVersion := tnm.ScalaVersion.prev

name := "docile-charge-point"

organization := "de.reinier"

mainClass := Some("chargepoint.docile.RunTest")

connectInput in run := true

libraryDependencies ++= Seq(
  "biz.enef"              %% "slogging"       % "0.6.0",
  "com.thenewmotion.ocpp" %% "ocpp-j-api"     % "6.0.0",
  "com.typesafe.akka"     %% "akka-actor"     % "2.5.6",
  "org.rogach"            %% "scallop"        % "3.1.0",
  "org.scala-lang"         % "scala-compiler" % "2.11.11",
  "org.slf4j"              % "slf4j-nop"      % "1.7.25"
)
