enablePlugins(OssLibPlugin)

scalaVersion := tnm.ScalaVersion.prev

name := "docile-charge-point"

organization := "de.reinier"

mainClass := Some("chargepoint.docile.RunTest")

connectInput in run := true

libraryDependencies ++= Seq(
  "com.thenewmotion.ocpp" %% "ocpp-j-api"     % "6.0.0-SNAPSHOT",
  "org.typelevel"         %% "cats-core"      % "1.0.0-MF",
  "com.typesafe.akka"     %% "akka-actor"     % "2.5.6",
  "org.rogach"            %% "scallop"        % "3.1.0",
  "org.scala-lang"         % "scala-compiler" % "2.11.11",
  "biz.enef"              %% "slogging"       % "0.6.0",
  "org.slf4j"              % "slf4j-nop"      % "1.7.25"
)
