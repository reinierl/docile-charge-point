enablePlugins(OssLibPlugin)

scalaVersion := tnm.ScalaVersion.prev

name := "docile-charge-point"

organization := "de.reinier"

mainClass := Some("chargepoint.docile.Main")

assemblyJarName in assembly := "docile.jar"

connectInput in run := true

libraryDependencies ++= Seq(
  "biz.enef"              %% "slogging"       % "0.6.0",
  "com.lihaoyi"            % "ammonite"       % "1.0.3"    cross CrossVersion.full,
  "com.thenewmotion.ocpp" %% "ocpp-j-api"     % "6.0.1",
  "org.rogach"            %% "scallop"        % "3.1.1",
  "org.scala-lang"         % "scala-compiler" % "2.11.11",
  "org.slf4j"              % "slf4j-nop"      % "1.7.25"
)
