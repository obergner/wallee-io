import Dependencies._

name := "wallee-io"

lazy val walleeIo = project
  .in(file("."))
  .settings(unidocSettings: _*)
  .enablePlugins(AutomateHeaderPlugin, GitVersioning, GitBranchPrompt)
  .aggregate(spi, shared, protocol, codec, connection, server)

lazy val spi = project
  .in(file("spi"))
  .settings(libraryDependencies ++= spiDeps)

lazy val shared = project
  .in(file("shared"))
  .settings(libraryDependencies ++= sharedDeps)
  .dependsOn(spi)

lazy val protocol = project
  .in(file("protocol"))
  .settings(libraryDependencies ++= protocolDeps)

lazy val codec = project
  .in(file("codec"))
  .settings(libraryDependencies ++= codecDeps)
  .dependsOn(shared, protocol)

lazy val connection = project
  .in(file("connection"))
  .settings(libraryDependencies ++= connectionDeps)
  .dependsOn(spi, shared, protocol, codec)

lazy val playground = project
  .in(file("playground"))
  .settings(libraryDependencies ++= playgroundDeps)
  .dependsOn(spi, protocol, codec, connection)

lazy val server = project
  .in(file("server"))
  .settings(libraryDependencies ++= serverDeps)
  .dependsOn(spi, protocol, codec, connection)

initialCommands := """|import io.wallee._""".stripMargin
