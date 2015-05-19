lazy val walleeIo = project
  .in(file("."))
  .enablePlugins(AutomateHeaderPlugin, GitVersioning, GitBranchPrompt)
  .aggregate(common, protocol, codec)

name := "wallee-io"

lazy val common = project
  .in(file("common"))

lazy val protocol = project
  .in(file("protocol"))

lazy val codec = project
  .in(file("codec"))
  .dependsOn(protocol)

libraryDependencies ++= List(
  Library.akkaActor,
  Library.akkaContrib,
  Library.akkaLog4j,
  Library.log4jCore,
  Library.metricsScala,
  Library.akkaTestkit          % "test",
  Library.scalaTest            % "test"
)

initialCommands := """|import io.wallee._""".stripMargin
