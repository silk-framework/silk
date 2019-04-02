import sbt._

//////////////////////////////////////////////////////////////////////////////
// Common Settings
//////////////////////////////////////////////////////////////////////////////

concurrentRestrictions in Global += Tags.limit(Tags.Test, 1)

lazy val commonSettings = Seq(
  organization := "org.silkframework",
  version := "3.0.0-SNAPSHOT",
  // Building
  scalaVersion := "2.11.11",
  publishTo := {
    val artifactory = "https://artifactory.eccenca.com/"
    if (isSnapshot.value) {
      Some("snapshots" at artifactory + "maven-ecc-snapshot")
    } else {
      Some("releases" at artifactory + "maven-ecc-release")
    }
  },
  // Testing
  libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.6" % "test",
  libraryDependencies += "net.codingwell" %% "scala-guice" % "4.0.0" % "test",
  libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.11",
  libraryDependencies += "org.mockito" % "mockito-all" % "1.9.5" % "test",
  libraryDependencies += "com.google.inject" % "guice" % "4.0" % "test",
  libraryDependencies += "javax.inject" % "javax.inject" % "1",
  testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-u", "target/test-reports"),

  dependencyOverrides ++= Set(
    // This overrides version 1.9.36 of async-http-client in Play 2.4.8, which has a bug. See Ticket #12089, TODO: Remove after next Play update
    "com.ning" % "async-http-client" % "1.9.39",
    "com.ning" % "async-http-client" % "1.9.39" % "test",
    "com.google.guava" % "guava" % "18.0",
    "com.google.inject" % "guice" % "4.0",
    "org.apache.thrift" % "libthrift" % "0.9.3",
    "io.netty" % "netty" % "3.10.5.Final",
    "com.fasterxml.jackson.core" % "jackson-databind" % "2.6.5",
    "com.google.code.findbugs" % "jsr305" % "3.0.0",
    "javax.servlet" % "javax.servlet-api" % "3.1.0" // FIXME: Needs to be re-evaluated when changing the Fuseki version (currently 3.7.0), comes from jetty-servlets 9.4.7.v20170914
  ),

  // The assembly plugin cannot resolve multiple dependencies to commons logging
  assemblyMergeStrategy in assembly := {
    case PathList("org", "apache", "commons", "logging",  xs @ _*) => MergeStrategy.first
    case PathList(xs @ _*) if xs.last endsWith ".class" => MergeStrategy.first
    case PathList(xs @ _*) if xs.last endsWith ".xsd" => MergeStrategy.first
    case PathList(xs @ _*) if xs.last endsWith ".dtd" => MergeStrategy.first
    case other =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(other)
  },
  // Use dependency injected routes in Play modules
  routesGenerator := InjectedRoutesGenerator
)

//////////////////////////////////////////////////////////////////////////////
// Core Modules
//////////////////////////////////////////////////////////////////////////////

lazy val core = (project in file("silk-core"))
  .settings(commonSettings: _*)
  .settings(
    name := "Silk Core",
    libraryDependencies += "com.typesafe" % "config" % "1.3.0", // Should always use the same version as the Play Framework dependency
    libraryDependencies += "com.rockymadden.stringmetric" % "stringmetric-core_2.11" % "0.27.4",
    libraryDependencies += "com.thoughtworks.paranamer" % "paranamer" % "2.7",
    // Additional scala standard libraries
    libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.5",
    libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value,
    libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4",
    libraryDependencies += "commons-io" % "commons-io" % "2.4"
  )

lazy val rules = (project in file("silk-rules"))
  .dependsOn(core % "test->test;compile->compile", pluginsCsv % "compile->compile")
  .settings(commonSettings: _*)
  .settings(
    name := "Silk Rules",
    libraryDependencies += "org.postgresql" % "postgresql" % "42.2.5",
    libraryDependencies += "org.apache.jena" % "jena-core" % "3.7.0" exclude("org.slf4j", "slf4j-log4j12"),
    libraryDependencies += "org.apache.jena" % "jena-arq" % "3.7.0" exclude("org.slf4j", "slf4j-log4j12")
  )

lazy val learning = (project in file("silk-learning"))
  .dependsOn(rules, workspace)
  .settings(commonSettings: _*)
  .settings(
    name := "Silk Learning"
  )

lazy val workspace = (project in file("silk-workspace"))
  .dependsOn(rules, core % "test->test", pluginsJson % "test->compile;test->test")
  .aggregate(rules)
  .settings(commonSettings: _*)
  .settings(
    name := "Silk Workspace",
    libraryDependencies += "com.typesafe.play" % "play-ws_2.11" % "2.4.8"
  )

//////////////////////////////////////////////////////////////////////////////
// Plugins
//////////////////////////////////////////////////////////////////////////////

lazy val pluginsRdf = (project in file("silk-plugins/silk-plugins-rdf"))
  .dependsOn(rules, workspace % "test->test;compile->compile", core % "test->test;compile->compile", pluginsCsv % "test->compile")
  .settings(commonSettings: _*)
  .settings(
    name := "Silk Plugins RDF",
    libraryDependencies += "org.apache.jena" % "jena-fuseki-embedded" % "3.7.0" % "test"
  )

lazy val pluginsCsv = (project in file("silk-plugins/silk-plugins-csv"))
  .dependsOn(core)
  .settings(commonSettings: _*)
  .settings(
    name := "Silk Plugins CSV",
    libraryDependencies += "com.univocity" % "univocity-parsers" % "2.7.6"
  )

lazy val pluginsXml = (project in file("silk-plugins/silk-plugins-xml"))
  .dependsOn(core, workspace % "test -> compile;test -> test", pluginsRdf % "test->compile")
  .settings(commonSettings: _*)
  .settings(
    name := "Silk Plugins XML",
    libraryDependencies += "net.sf.saxon" % "Saxon-HE" % "9.8.0-6"
  )

lazy val pluginsJson = (project in file("silk-plugins/silk-plugins-json"))
  .dependsOn(core)
  .settings(commonSettings: _*)
  .settings(
    name := "Silk Plugins JSON",
    libraryDependencies += "com.typesafe.play" % "play-json_2.11" % "2.4.8",
    libraryDependencies += "com.fasterxml.jackson.core" % "jackson-core" % "2.8.6"
  )

lazy val pluginsSpatialTemporal = (project in file("silk-plugins/silk-plugins-spatial-temporal"))
  .dependsOn(rules)
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

lazy val pluginsAsian = (project in file("silk-plugins/silk-plugins-asian"))
  .dependsOn(rules)
  .settings(commonSettings: _*)
  .settings(
    name := "Silk Plugins Asian"
  )

lazy val serializationJson = (project in file("silk-plugins/silk-serialization-json"))
  .dependsOn(core, rules, workspace)
  .settings(commonSettings: _*)
  .settings(
    name := "Silk Serialization JSON",
    libraryDependencies += "com.typesafe.play" % "play-json_2.11" % "2.4.8"
  )

// Aggregate all plugins
// pluginsSpatialTemporal has been removed as it uses dependencies from external unreliable repositories
lazy val plugins = (project in file("silk-plugins"))
  .dependsOn(pluginsRdf, pluginsCsv, pluginsXml, pluginsJson, pluginsAsian, serializationJson)
  .aggregate(pluginsRdf, pluginsCsv, pluginsXml, pluginsJson, pluginsAsian, serializationJson)
  .settings(commonSettings: _*)
  .settings(
    name := "Silk Plugins"
  )

//////////////////////////////////////////////////////////////////////////////
// Silk React Components
//////////////////////////////////////////////////////////////////////////////

val silkWorkbenchRoot: Def.Initialize[File] = Def.setting {
  new File(baseDirectory.value, "../silk-workbench")
}

val silkDistRoot: Def.Initialize[File] = Def.setting {
  new File(baseDirectory.value, "../silk-workbench/silk-workbench-core/public/libs/silk-react-components")
}

/** list of vendor libs we maintain in the package.json */
val LIST_OF_VENDORS = "dialog-polyfill jquery jquery-migrate jsplumb jstree lodash mark.js @eccenca/material-design-lite mdl-selectfield twbs-pagination"

val checkJsBuildTools = taskKey[Unit]("Check the commandline tools yarn")
val buildSilkReact = taskKey[Unit]("Builds silk React module")

lazy val reactComponents = (project in file("silk-react-components"))
  .settings(commonSettings: _*)
  .settings(
    name := "Silk React Components",
    //////////////////////////////////////////////////////////////////////////////
    // Silk React build pipeline
    //////////////////////////////////////////////////////////////////////////////
    /** Check that all necessary build tool for the JS pipeline are available */
    checkJsBuildTools := {
      ReactBuildHelper.checkReactBuildTool()
    },
    // Run when building silk react
    /** Build Silk React */
    buildSilkReact := {
      checkJsBuildTools.value // depend on check
      val reactWatchConfig = WatchConfig(new File(baseDirectory.value, "src"), fileRegex = """\.(jsx|js|scss|json)$""")
      def distFile(name: String): File = new File(baseDirectory.value, "dist/" + name)
      if (Watcher.staleTargetFiles(reactWatchConfig, Seq(distFile("main.js"), distFile("style.css")))) {
        ReactBuildHelper.buildReactComponents(baseDirectory.value, silkDistRoot.value, "Silk")
      }
      val silkReactWorkbenchRoot = new File(baseDirectory.value, "silk-workbench")
      val changedJsFiles = Watcher.filesChanged(WatchConfig(silkReactWorkbenchRoot, fileRegex = """\.js$"""))
      if(changedJsFiles.nonEmpty) {
        // Transpile JavaScript files to ES5
        for(file <- changedJsFiles) {
          val relativePath = silkReactWorkbenchRoot.toURI().relativize(file.toURI()).getPath()
          val targetFile = new File(silkWorkbenchRoot.value, relativePath)
          ReactBuildHelper.transpileJavaScript(baseDirectory.value, file, targetFile)
        }
      }
    },
    (compile in Compile) := ((compile in Compile) dependsOn buildSilkReact).value,
    watchSources ++= { // Watch all files under the silk-react-components/src directory for changes
      val paths = for(path <- Path.allSubpaths(baseDirectory.value / "src")) yield {
        path._1
      }
      paths.toSeq
    },
    watchSources ++= { // Watch all JavaScript files under the silk-react-components/silk-workbench directory for changes
      val paths = for(path <- Path.allSubpaths(baseDirectory.value / "silk-workbench")) yield {
        path._1
      }
      paths.toSeq.filter(_.getName.endsWith(".js"))
    }
  )

//////////////////////////////////////////////////////////////////////////////
// Workbench
//////////////////////////////////////////////////////////////////////////////

lazy val workbenchCore = (project in file("silk-workbench/silk-workbench-core"))
  .enablePlugins(PlayScala)
  .enablePlugins(BuildInfoPlugin)
  .dependsOn(workspace, workspace % "test -> test", core % "test->test", serializationJson, reactComponents)
  .aggregate(workspace, reactComponents)
  .settings(commonSettings: _*)
  .settings(
    name := "Silk Workbench Core",
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "org.silkframework.buildInfo",
    // Play filters (CORS filter etc.)
    libraryDependencies += filters,
    libraryDependencies += "org.scalatestplus" % "play_2.11" % "1.4.0" % "test"
  )

lazy val workbenchWorkspace = (project in file("silk-workbench/silk-workbench-workspace"))
  .enablePlugins(PlayScala)
  .dependsOn(workbenchCore % "compile->compile;test->test", pluginsRdf, pluginsCsv % "test->compile", pluginsXml % "test->compile")
  .aggregate(workbenchCore)
  .settings(commonSettings: _*)
  .settings(
    name := "Silk Workbench Workspace"
  )

lazy val workbenchRules = (project in file("silk-workbench/silk-workbench-rules"))
  .enablePlugins(PlayScala)
  .dependsOn(workbenchWorkspace % "compile->compile;test->test", pluginsXml % "test->compile", pluginsJson % "test->compile", learning)
  .aggregate(workbenchWorkspace)
  .settings(commonSettings: _*)
  .settings(
    name := "Silk Workbench Rules"
  )

lazy val workbenchWorkflow = (project in file("silk-workbench/silk-workbench-workflow"))
  .enablePlugins(PlayScala)
  .dependsOn(workbenchWorkspace % "compile->compile;test->test", workbenchRules, serializationJson)
  .aggregate(workbenchWorkspace)
  .settings(commonSettings: _*)
  .settings(
    name := "Silk Workbench Workflow"
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
  .dependsOn(rules, workspace, plugins, core % "test->test")
  .aggregate(rules, workspace, plugins)
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

lazy val mapreduce = (project in file("silk-tools/silk-mapreduce"))
  .dependsOn(core, plugins)
  .aggregate(core, plugins)
  .settings(commonSettings: _*)
  .settings(
    name := "Silk MapReduce"
  )

//////////////////////////////////////////////////////////////////////////////
// Root
//////////////////////////////////////////////////////////////////////////////

lazy val root = (project in file("."))
  .aggregate(core, plugins, mapreduce, singlemachine, learning, workspace, workbench)
  .settings(commonSettings: _*)
