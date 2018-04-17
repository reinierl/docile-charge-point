

enablePlugins(OssLibPlugin)

lazy val commonSettings = Seq(
  organization := "com.newmotion",
  version := "0.0.1-SNAPSHOT",
  scalaVersion := tnm.ScalaVersion.prev,
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint")
)

lazy val root = (project in file(".")).
  settings(
    commonSettings,
    name := "docile-charge-point",
    mainClass := Some("chargepoint.docile.Main"),
    assemblyJarName in assembly := "docile.jar",
    connectInput in run := true,
    libraryDependencies ++= deps
  )

lazy val lambda = (project in file("aws-lambda")).
  dependsOn(root).
  settings(
    commonSettings,
    name := "lambda-docile-charge-point",
    retrieveManaged := true,
    libraryDependencies ++= deps,
    mainClass := Some("chargepoint.docile.Lambda"),
    assemblyJarName in assembly := "docile-lambda.jar"
  )

assemblyJarName in assembly := "docile.jar"

connectInput in run := true

libraryDependencies ++= Seq(
  "com.lihaoyi"            % "ammonite"         % "1.0.3"    cross CrossVersion.full,
  "com.thenewmotion.ocpp" %% "ocpp-j-api"       % "7.0.0",
  "org.rogach"            %% "scallop"          % "3.1.1",
  "org.scala-lang"         % "scala-compiler"   % "2.11.11",

  "biz.enef"              %% "slogging"         % "0.6.1",
  "biz.enef"              %% "slogging-slf4j"   % "0.6.1",
  "org.slf4j"              % "slf4j-api"        % "1.7.25",
  "ch.qos.logback"         % "logback-classic"  % "1.2.3",

  "org.specs2"            %% "specs2-core"      % "4.0.2"    % "test"
)

