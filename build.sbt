enablePlugins(OssLibPlugin)

scalaVersion := tnm.ScalaVersion.prev

name := "docile-charge-point"

organization := "de.reinier"

mainClass := Some("de.reinier.ocpp.scriptable.RunTest")

libraryDependencies ++= Seq(
  "com.thenewmotion.ocpp" %% "ocpp-j-api"     % "6.0.0-beta3",
  "org.typelevel"         %% "cats-core"      % "1.0.0-MF",
  "com.typesafe.akka"     %% "akka-actor"     % "2.5.6",
  "org.rogach"            %% "scallop"        % "3.1.0",
  "org.scala-lang"         % "scala-compiler" % "2.11.11"
)

