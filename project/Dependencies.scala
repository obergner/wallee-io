import sbt._

object Version {
  val akka                 = "2.3.11"
  val akkaLog4j            = "0.2.0"
  val akkaStreams          = "1.0-RC2"
  val log4j                = "2.3"
  val scala                = "2.11.6"
  val metricsScala         = "3.5.1_a2.3"

  val scalaTest            = "2.2.4"
}

object Library {
  val akkaActor            = "com.typesafe.akka"        %% "akka-actor"                           % Version.akka
  val akkaContrib          = "com.typesafe.akka"        %% "akka-contrib"                         % Version.akka
  val akkaStreams          = "com.typesafe.akka"        %% "akka-stream-experimental"             % Version.akkaStreams
  val akkaLog4j            = "de.heikoseeberger"        %% "akka-log4j"                           % Version.akkaLog4j
  val log4jCore            = "org.apache.logging.log4j" %  "log4j-core"                           % Version.log4j
  val metricsScala         = "nl.grons"                 %% "metrics-scala"                        % Version.metricsScala

  val akkaTestkit          = "com.typesafe.akka"        %% "akka-testkit"                         % Version.akka
  val akkaStreamsTestkit   = "com.typesafe.akka"        %% "akka-stream-testkit-experimental"     % Version.akkaStreams
  val scalaTest            = "org.scalatest"            %% "scalatest"                            % Version.scalaTest
}

object Dependencies {
  val commonDeps           = Seq(Library.log4jCore, Library.akkaLog4j, Library.scalaTest % Test)

  val sharedDeps           = commonDeps

  val protocolDeps         = commonDeps

  val codecDeps            = commonDeps ++ Seq(Library.akkaActor, Library.akkaStreams, Library.akkaTestkit % Test, Library.akkaStreamsTestkit % Test)

  val connectionDeps       = commonDeps

  val playgroundDeps       = commonDeps

  val spiDeps              = commonDeps
}
