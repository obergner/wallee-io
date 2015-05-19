import Dependencies._

name := "wallee-io"

lazy val walleeIo = project
  .in(file("."))
  .enablePlugins(AutomateHeaderPlugin, GitVersioning, GitBranchPrompt)
  .aggregate(shared, protocol, codec)

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

initialCommands := """|import io.wallee._""".stripMargin
