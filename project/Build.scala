import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import com.typesafe.sbt.{GitPlugin, SbtScalariform}
import de.heikoseeberger.sbtheader.license.Apache2_0
import de.heikoseeberger.sbtheader.{AutomateHeaderPlugin, HeaderPlugin}
import sbt.Keys._
import sbt._
import spray.revolver.RevolverPlugin.Revolver

import scalariform.formatter.preferences.{AlignParameters, AlignSingleLineCaseStatements, DoubleIndentClassDeclaration}

object Build extends AutoPlugin {

  override def requires = plugins.JvmPlugin && HeaderPlugin && GitPlugin

  override def trigger = allRequirements

  override def projectSettings =
  // Core settings
    List(
      organization := "io.wallee",
      licenses +=("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")),
      scalaVersion := Version.scala,
      crossScalaVersions := List(scalaVersion.value),
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
        "-Xmx1024m",
        "-Xms256m",
        "-Xss10m"
      ),
      javaOptions ++= Seq(
        "-Xms256m",
        "-Xmx1536m",
        "-Djava.awt.headless=true"
      ),
      unmanagedSourceDirectories.in(Compile) := List(scalaSource.in(Compile).value),
      unmanagedSourceDirectories.in(Test) := List(scalaSource.in(Test).value)
    ) ++
      // Scalariform settings
      SbtScalariform.scalariformSettings ++
      List(
        ScalariformKeys.preferences := ScalariformKeys.preferences.value
          .setPreference(AlignSingleLineCaseStatements, true)
          .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 100)
          .setPreference(DoubleIndentClassDeclaration, true)
          .setPreference(AlignParameters, true)
      ) ++
      // Git settings
      List(
        GitPlugin.autoImport.git.baseVersion := "0.1.0"
      ) ++
      // Header settings
      AutomateHeaderPlugin.automateFor(Compile, Test) ++
      List(
        HeaderPlugin.autoImport.headers := Map("scala" -> Apache2_0("2015", "Olaf Bergner"))
      ) ++
      HeaderPlugin.settingsFor(Compile, Test) ++
      // Revolver settings
      Revolver.settings
}
