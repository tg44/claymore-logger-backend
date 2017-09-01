import org.scalafmt.bootstrap.ScalafmtBootstrap

name := "claymore-logger-backend"

version := "1.0"

scalaVersion := "2.12.2"

enablePlugins(UniversalPlugin)
enablePlugins(JavaAppPackaging)
resourceDirectory in Compile := (resourceDirectory in Compile).value

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % "2.5.3",
  "com.typesafe.akka" %% "akka-stream-testkit" % "2.5.3" % Test
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % "10.0.9",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.9",
  "com.typesafe.akka" %% "akka-http-testkit" % "10.0.9" % Test
)

libraryDependencies ++= Seq(
  "com.google.guava" % "guava" % "23.0",
  "com.github.pureconfig" %% "pureconfig" % "0.7.2",
  "org.scalatest" %% "scalatest" % "3.0.3" % Test,
  "org.mockito" % "mockito-all" % "1.10.19" % Test,
  "com.pauldijou" %% "jwt-json4s-native" % "0.12.0",
  "org.json4s" %% "json4s-native" % "3.5.0",
  "org.mongodb.scala" %% "mongo-scala-driver" % "2.1.0",
  "com.github.simplyscala" %% "scalatest-embedmongo" % "0.2.4" % Test,
  "io.netty" % "netty-all" % "4.1.15.Final",
  "org.scaldi" %% "scaldi" % "0.5.8",
  "org.scaldi" %% "scaldi-akka" % "0.5.8"
)

val format = TaskKey[Unit]("format", "Run format.") //this will create a new sbt task with no parameters
format := ScalafmtBootstrap.main(Seq("--non-interactive")) //this will assign a plugin's main class to the format

val formatTest = TaskKey[Unit]("formatTest", "Run formatTest.") //this will create a new sbt task with no parameters
formatTest := ScalafmtBootstrap.main(Seq("--non-interactive", "--test")) //this will assign a plugin's main class to the formatTest

parallelExecution in Test := false //I hate this but TravisCi not like the embedmongo the way I use :(
