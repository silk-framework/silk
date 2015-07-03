import com.typesafe.sbt.packager.Keys._
import sbt.Keys._

//////////////////////////////////////////////////////////////////////////////
// Common Settings
//////////////////////////////////////////////////////////////////////////////

lazy val commonSettings = Seq(
  organization := "com.silk-framework",
  version := "2.6.1-SNAPSHOT",
  // Building
  scalaVersion := "2.11.6",
  javacOptions := Seq("-source", "1.7", "-target", "1.7"),
  scalacOptions += "-target:jvm-1.7",
  // Testing
  libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test",
  libraryDependencies += "junit" % "junit" % "4.11" % "test",
  testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-u", "target/test-reports")
)

//////////////////////////////////////////////////////////////////////////////
// Core Modules
//////////////////////////////////////////////////////////////////////////////

lazy val core = (project in file("silk-core"))
    .settings(commonSettings: _*)
    .settings(
      libraryDependencies += "com.rockymadden.stringmetric" % "stringmetric-core_2.11" % "0.27.4",
      libraryDependencies += "com.thoughtworks.paranamer" % "paranamer" % "2.7",
      libraryDependencies += "org.clapper" % "classutil_2.11" % "1.0.5" exclude("org.slf4j", "slf4j-log4j12"),
      // Additional scala standard libraries
      libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.4",
      libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4"
    )

lazy val learning = (project in file("silk-learning"))
    .dependsOn(core)
    .settings(commonSettings: _*)

//////////////////////////////////////////////////////////////////////////////
// Plugins
//////////////////////////////////////////////////////////////////////////////

lazy val pluginsJena = (project in file("silk-plugins/silk-plugins-jena"))
    .dependsOn(core)
    .settings(commonSettings: _*)
    .settings(
      libraryDependencies += "org.apache.jena" % "jena-core" % "2.13.0", // exclude("org.slf4j", "slf4j-log4j12")
      libraryDependencies += "org.apache.jena" % "jena-arq" % "2.13.0" // exclude("org.slf4j", "slf4j-log4j12")
    )

lazy val pluginsJson = (project in file("silk-plugins/silk-plugins-json"))
    .dependsOn(core)
    .settings(commonSettings: _*)
    .settings(
      libraryDependencies += "com.typesafe.play" % "play-json_2.11" % "2.3.9"
    )

lazy val pluginsSpatialTemporal = (project in file("silk-plugins/silk-plugins-spatial-temporal"))
    .dependsOn(core)
    .settings(commonSettings: _*)
    .settings(
      //Spatial Extensions
      libraryDependencies += "com.vividsolutions" % "jts" % "1.13",
      libraryDependencies += "org.jvnet.ogc" % "ogc-tools-gml-jts" % "1.0.3",
      libraryDependencies += "org.geotools" % "gt-opengis" % "13.1",
      libraryDependencies += "org.geotools" % "gt-referencing" % "13.1",
      libraryDependencies += "org.geotools" % "gt-jts-wrapper" % "13.1",
      libraryDependencies += "org.geotools" % "gt-epsg-wkt" % "13.1",
      resolvers += "OpenGeo Maven Repository" at "http://download.osgeo.org/webdav/geotools/"
    )

lazy val plugins = (project in file("silk-plugins"))
    .dependsOn(pluginsJena, pluginsJson, pluginsSpatialTemporal)
    .settings(commonSettings: _*)

//////////////////////////////////////////////////////////////////////////////
// Workbench
//////////////////////////////////////////////////////////////////////////////

lazy val workspace = (project in file("silk-workspace"))
    .dependsOn(core, plugins, learning)
    .settings(commonSettings: _*)

lazy val workbenchCore = (project in file("silk-workbench/silk-workbench-core"))
    .enablePlugins(PlayScala)
    .dependsOn(workspace)
    .aggregate(workspace)
    .settings(commonSettings: _*)

lazy val workbenchWorkspace = (project in file("silk-workbench/silk-workbench-workspace"))
    .enablePlugins(PlayScala)
    .dependsOn(workbenchCore)
    .aggregate(workbenchCore)
    .settings(commonSettings: _*)

lazy val workbenchWorkflow = (project in file("silk-workbench/silk-workbench-workflow"))
    .enablePlugins(PlayScala)
    .dependsOn(workbenchCore)
    .aggregate(workbenchCore)
    .settings(commonSettings: _*)

lazy val workbenchRules = (project in file("silk-workbench/silk-workbench-rules"))
    .enablePlugins(PlayScala)
    .dependsOn(workbenchWorkspace)
    .aggregate(workbenchWorkspace)
    .settings(commonSettings: _*)

lazy val workbench = (project in file("silk-workbench"))
    .enablePlugins(PlayScala)
    .dependsOn(workbenchWorkspace, workbenchRules)
    .aggregate(workbenchWorkspace, workbenchRules)
    .settings(commonSettings: _*)
    .settings(com.github.play2war.plugin.Play2WarPlugin.play2WarSettings: _*)
    .settings(
      // War Packaging
      com.github.play2war.plugin.Play2WarKeys.servletVersion := "3.0",
      // Linux Packaging, Uncomment to generate Debian packages that register the Workbench as an Upstart service
      // packageArchetype.java_server
      version in Debian := "2.6.1",
      maintainer := "Robert Isele <silk-discussion@googlegroups.com>",
      packageSummary := "The Silk framework is a tool for discovering relationships between data items within different Linked Data sources.",
      packageDescription := "The Silk framework is a tool for discovering relationships between data items within different Linked Data sources."
    )

//////////////////////////////////////////////////////////////////////////////
// Tools
//////////////////////////////////////////////////////////////////////////////

lazy val singlemachine = (project in file("silk-tools/silk-singlemachine"))
  .dependsOn(core, plugins)
  .settings(commonSettings: _*)

//////////////////////////////////////////////////////////////////////////////
// Root
//////////////////////////////////////////////////////////////////////////////

lazy val root = (project in file("."))
  .aggregate(core, plugins, singlemachine, learning, workspace, workbench)
  .settings(commonSettings: _*)
