import sbt._

object Version {
  val akka = "2.5.8"
  val akkaLog4j = "1.6.0"
  val log4j = "2.10.0"
  val scala = "2.12.4"
  val metricsScala = "3.5.9_a2.4"

  val scalaTest = "3.0.4"
}

object Library {
  val akkaActor = "com.typesafe.akka" %% "akka-actor" % Version.akka
  val akkaStream = "com.typesafe.akka" %% "akka-stream" % Version.akka
  val akkaContrib = "com.typesafe.akka" %% "akka-contrib" % Version.akka
  val akkaLog4j = "de.heikoseeberger" %% "akka-log4j" % Version.akkaLog4j
  val log4jCore = "org.apache.logging.log4j" % "log4j-core" % Version.log4j
  val metricsScala = "nl.grons" %% "metrics-scala" % Version.metricsScala

  val akkaTestkit = "com.typesafe.akka" %% "akka-testkit" % Version.akka
  val akkaStreamTestkit = "com.typesafe.akka" %% "akka-stream-testkit" % Version.akka
  val scalaTest = "org.scalatest" %% "scalatest" % Version.scalaTest
}

object Dependencies {
  val commonDeps = Seq(Library.akkaActor,
    Library.akkaStream,
    Library.log4jCore,
    Library.akkaLog4j,
    Library.scalaTest % Test,
    Library.akkaTestkit % Test,
    Library.akkaStreamTestkit % Test)

  val sharedDeps = commonDeps

  val protocolDeps = commonDeps

  val codecDeps = commonDeps

  val connectionDeps = commonDeps

  val spiDeps = commonDeps

  val serverDeps = commonDeps

  val playgroundDeps = commonDeps
}
