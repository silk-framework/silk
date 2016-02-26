import com.typesafe.sbt.packager.Keys._
import sbt.Keys._

//////////////////////////////////////////////////////////////////////////////
// Common Settings
//////////////////////////////////////////////////////////////////////////////

lazy val commonSettings = Seq(
  organization := "org.silkframework",
  version := "2.7.2",
  // Building
  scalaVersion := "2.11.7",
  javacOptions := Seq("-source", "1.7", "-target", "1.7"),
  scalacOptions += "-target:jvm-1.7",
  // Testing
  libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.6" % "test",
  testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-u", "target/test-reports")
)

//////////////////////////////////////////////////////////////////////////////
// Core Modules
//////////////////////////////////////////////////////////////////////////////

lazy val core = (project in file("silk-core"))
  .settings(commonSettings: _*)
  .settings(
    name := "Silk Core",
    libraryDependencies += "com.typesafe" % "config" % "1.2.1", // Should always use the same version as the Play Framework dependency
    libraryDependencies += "com.rockymadden.stringmetric" % "stringmetric-core_2.11" % "0.27.4",
    libraryDependencies += "com.thoughtworks.paranamer" % "paranamer" % "2.7",
    // Additional scala standard libraries
    libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.5",
    libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4"
  )

lazy val learning = (project in file("silk-learning"))
  .dependsOn(core)
  .settings(commonSettings: _*)
  .settings(
    name := "Silk Learning"
  )

lazy val workspace = (project in file("silk-workspace"))
  .dependsOn(core, learning)
  .aggregate(core, learning)
  .settings(commonSettings: _*)
  .settings(
    name := "Silk Workspace"
  )

//////////////////////////////////////////////////////////////////////////////
// Plugins
//////////////////////////////////////////////////////////////////////////////

lazy val pluginsRdf = (project in file("silk-plugins/silk-plugins-rdf"))
  .dependsOn(core)
  .settings(commonSettings: _*)
  .settings(
    name := "Silk Plugins RDF",
    libraryDependencies += "org.apache.jena" % "jena-core" % "2.13.0" exclude("org.slf4j", "slf4j-log4j12"),
    libraryDependencies += "org.apache.jena" % "jena-arq" % "2.13.0" exclude("org.slf4j", "slf4j-log4j12")
  )

lazy val pluginsCsv = (project in file("silk-plugins/silk-plugins-csv"))
  .dependsOn(core)
  .settings(commonSettings: _*)
  .settings(
    name := "Silk Plugins CSV",
    libraryDependencies += "com.univocity" % "univocity-parsers" % "1.5.6"
  )

lazy val pluginsXml = (project in file("silk-plugins/silk-plugins-xml"))
  .dependsOn(core)
  .settings(commonSettings: _*)
  .settings(
    name := "Silk Plugins XML"
  )

lazy val pluginsJson = (project in file("silk-plugins/silk-plugins-json"))
  .dependsOn(core)
  .settings(commonSettings: _*)
  .settings(
    name := "Silk Plugins JSON",
    libraryDependencies += "com.typesafe.play" % "play-json_2.11" % "2.3.10"
  )

lazy val pluginsSpatialTemporal = (project in file("silk-plugins/silk-plugins-spatial-temporal"))
  .dependsOn(core)
  .settings(commonSettings: _*)
  .settings(
    name := "Silk Plugins SpatialTemporal",
    libraryDependencies += "com.vividsolutions" % "jts" % "1.13",
    libraryDependencies += "org.jvnet.ogc" % "ogc-tools-gml-jts" % "1.0.3",
    libraryDependencies += "org.geotools" % "gt-opengis" % "13.1",
    libraryDependencies += "org.geotools" % "gt-referencing" % "13.1",
    libraryDependencies += "org.geotools" % "gt-jts-wrapper" % "13.1",
    libraryDependencies += "org.geotools" % "gt-epsg-wkt" % "13.1",
    resolvers += "OpenGeo Maven Repository" at "http://download.osgeo.org/webdav/geotools/"
  )

lazy val plugins = (project in file("silk-plugins"))
  .dependsOn(pluginsRdf, pluginsCsv, pluginsXml, pluginsJson, pluginsSpatialTemporal)
  .aggregate(pluginsRdf, pluginsCsv, pluginsXml, pluginsJson, pluginsSpatialTemporal)
  .settings(commonSettings: _*)
  .settings(
    name := "Silk Plugins"
  )

//////////////////////////////////////////////////////////////////////////////
// Workbench
//////////////////////////////////////////////////////////////////////////////

lazy val workbenchCore = (project in file("silk-workbench/silk-workbench-core"))
  .enablePlugins(PlayScala)
  .enablePlugins(BuildInfoPlugin)
  .dependsOn(workspace)
  .aggregate(workspace)
  .settings(commonSettings: _*)
  .settings(
    name := "Silk Workbench Core",
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "org.silkframework.buildInfo"
  )

lazy val workbenchWorkspace = (project in file("silk-workbench/silk-workbench-workspace"))
  .enablePlugins(PlayScala)
  .dependsOn(workbenchCore)
  .aggregate(workbenchCore)
  .settings(commonSettings: _*)
  .settings(
    name := "Silk Workbench Workspace"
  )

lazy val workbenchWorkflow = (project in file("silk-workbench/silk-workbench-workflow"))
  .enablePlugins(PlayScala)
  .dependsOn(workbenchWorkspace)
  .aggregate(workbenchWorkspace)
  .settings(commonSettings: _*)
  .settings(
    name := "Silk Workbench Workflow"
  )

lazy val workbenchRules = (project in file("silk-workbench/silk-workbench-rules"))
  .enablePlugins(PlayScala)
  .dependsOn(workbenchWorkspace, pluginsRdf)
  .aggregate(workbenchWorkspace)
  .settings(commonSettings: _*)
  .settings(
    name := "Silk Workbench Rules"
  )

lazy val workbench = (project in file("silk-workbench"))
    .enablePlugins(PlayScala)
    .dependsOn(workbenchWorkspace, workbenchRules, workbenchWorkflow, plugins)
    .aggregate(workbenchWorkspace, workbenchRules, workbenchWorkflow, plugins)
    .settings(commonSettings: _*)
    .settings(com.github.play2war.plugin.Play2WarPlugin.play2WarSettings: _*)
    .settings(
      name := "Silk Workbench",
      // War Packaging
      com.github.play2war.plugin.Play2WarKeys.servletVersion := "3.0",
      // Linux Packaging, Uncomment to generate Debian packages that register the Workbench as an Upstart service
      // packageArchetype.java_server
      version in Debian := "2.7.2",
      maintainer := "Robert Isele <silk-discussion@googlegroups.com>",
      packageSummary := "The Silk framework is a tool for discovering relationships between data items within different Linked Data sources.",
      packageDescription := "The Silk framework is a tool for discovering relationships between data items within different Linked Data sources."
    )

//////////////////////////////////////////////////////////////////////////////
// Tools
//////////////////////////////////////////////////////////////////////////////

lazy val singlemachine = (project in file("silk-tools/silk-singlemachine"))
  .dependsOn(core, plugins)
  .aggregate(core, plugins)
  .settings(commonSettings: _*)
  .settings(
    name := "Silk SingleMachine",
    libraryDependencies += "org.slf4j" % "slf4j-jdk14" % "1.7.13",
    // The assembly plugin cannot resolve multiple dependencies to commons logging
    assemblyMergeStrategy in assembly := {
      case PathList("org", "apache", "commons", "logging",  xs @ _*) => MergeStrategy.first
      case other =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(other)
    }
  )

//////////////////////////////////////////////////////////////////////////////
// Root
//////////////////////////////////////////////////////////////////////////////

lazy val root = (project in file("."))
  .aggregate(core, plugins, singlemachine, learning, workspace, workbench)
  .settings(commonSettings: _*)