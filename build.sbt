enablePlugins(OssLibPlugin)

scalaVersion := tnm.ScalaVersion.prev

name := "scriptable-ocpp"

organization := "de.reinier"

libraryDependencies ++= Seq(
  "com.thenewmotion.ocpp" %% "ocpp-json" % "4.3.0"
)

