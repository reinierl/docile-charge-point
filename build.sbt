enablePlugins(OssLibPlugin)

scalaVersion := tnm.ScalaVersion.prev

name := "scriptable-ocpp"

organization := "de.reinier"

mainClass := Some("de.reinier.ocpp.scriptable.RunTest")

libraryDependencies ++= Seq(
  "com.thenewmotion.ocpp" %% "ocpp-json" % "4.3.0",
  "org.typelevel" %% "cats-core" % "1.0.0-MF"
)

