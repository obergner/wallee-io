import Dependencies._

import scalariform.formatter.preferences._

name := "wallee-io"

lazy val commonSettings = Seq(
  organization := "io.wallee",
  organizationName := "io.wallee",
  startYear := Some(2017),
  licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")),
  scalaVersion := Version.scala,
  crossScalaVersions := List(scalaVersion.value),
  autoAPIMappings := true,
  scalacOptions ++= List(
    "-unchecked",
    "-deprecation",
    "-Xlint",
    "-language:_",
    "-target:jvm-1.8",
    "-encoding", "UTF-8"
  ),
  scalacOptions in(Compile, doc) ++= Seq(
    "-groups",
    "-implicits"
  ),
  javacOptions ++= Seq(
    "-source", "1.8",
    "-target", "1.8",
    "-Xlint:unchecked",
    "-deprecation",
    "-encoding", "UTF-8"
  ),
  javaOptions ++= Seq(
    "-Xms256m",
    "-Xmx1536m",
    "-Djava.awt.headless=true"
  ),
  unmanagedSourceDirectories.in(Compile) := List(scalaSource.in(Compile).value, javaSource.in(Compile).value),
  unmanagedSourceDirectories.in(Test) := List(scalaSource.in(Test).value),
  git.baseVersion := "0.1.0",
  scalariformPreferences := scalariformPreferences.value
    .setPreference(AlignSingleLineCaseStatements, true)
    .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 100)
    .setPreference(DoubleIndentConstructorArguments, true)
    .setPreference(AlignParameters, true)
    .setPreference(AlignArguments, true)
    .setPreference(MultilineScaladocCommentsStartOnFirstLine, true),
  wartremoverErrors in(Compile, compile) ++= Warts.allBut(
    Wart.Throw,
    Wart.Any,
    Wart.Nothing,
    Wart.Equals,
    Wart.Overloading,
    Wart.ImplicitConversion,
    Wart.ImplicitParameter
  )
)

lazy val walleeIo = project
  .in(file("."))
  .settings(
    commonSettings
  )
  .enablePlugins(AutomateHeaderPlugin, GitVersioning, GitBranchPrompt, ScalaUnidocPlugin)
  .aggregate(spi, shared, protocol, codec, connection, server)

lazy val spi = project
  .in(file("spi"))
  .settings(
    commonSettings,
    libraryDependencies ++= spiDeps
  )

lazy val shared = project
  .in(file("shared"))
  .settings(
    commonSettings,
    libraryDependencies ++= sharedDeps
  )
  .dependsOn(spi)

lazy val protocol = project
  .in(file("protocol"))
  .settings(
    commonSettings,
    libraryDependencies ++= protocolDeps
  )

lazy val codec = project
  .in(file("codec"))
  .settings(
    commonSettings,
    libraryDependencies ++= codecDeps
  )
  .dependsOn(shared, protocol)

lazy val connection = project
  .in(file("connection"))
  .settings(
    commonSettings,
    libraryDependencies ++= connectionDeps
  )
  .dependsOn(spi, shared, protocol, codec)

lazy val playground = project
  .in(file("playground"))
  .settings(
    commonSettings,
    libraryDependencies ++= playgroundDeps
  )
  .dependsOn(spi, protocol, codec, connection)

lazy val server = project
  .in(file("server"))
  .settings(
    commonSettings,
    libraryDependencies ++= serverDeps
  )
  .dependsOn(spi, protocol, codec, connection)

initialCommands := """|import io.wallee._""".stripMargin
