import Dependencies._

name := "wallee-io"

lazy val walleeIo = project
  .in(file("."))
  .settings(unidocSettings: _*)
  .enablePlugins(AutomateHeaderPlugin, GitVersioning, GitBranchPrompt)
  .aggregate(shared, protocol, codec, connection)

lazy val shared = project
  .in(file("shared"))
  .settings(libraryDependencies ++= sharedDeps)

lazy val protocol = project
  .in(file("protocol"))
  .settings(libraryDependencies ++= protocolDeps)

lazy val codec = project
  .in(file("codec"))
  .settings(libraryDependencies ++= codecDeps)
  .dependsOn(protocol)

lazy val connection = project
  .in(file("connection"))
  .settings(libraryDependencies ++= connectionDeps)
  .dependsOn(shared, protocol, codec)

lazy val playground = project
  .in(file("playground"))
  .settings(libraryDependencies ++= connectionDeps)
  .dependsOn(protocol, codec, connection)

initialCommands := """|import io.wallee._""".stripMargin
