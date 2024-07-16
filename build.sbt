import sbt.Keys.libraryDependencies
import sbt.{File, taskKey, *}

import java.io.FileWriter

//////////////////////////////////////////////////////////////////////////////
// Common Settings
//////////////////////////////////////////////////////////////////////////////

val NEXT_VERSION = "3.11.0"
val silkVersion = {
  val version = sys.env.getOrElse("GIT_DESCRIBE", NEXT_VERSION + "-SNAPSHOT")
  val configPath = "silk-workbench/silk-workbench-core/conf/reference.conf"
  // Check if silk is located inside a sub-folder
  val outFile = if(new File("silk/build.sbt").exists()) {
    new File("silk", configPath)
  } else {
    new File(configPath)
  }
  val os = new FileWriter(outFile)
  os.append(s"workbench.version = $version")
  os.flush()
  os.close()
  version
}

val buildReactExternally = {
  val result = sys.env.getOrElse("BUILD_REACT_EXTERNALLY", "FALSE").toLowerCase == "true"
  if(result) {
    println("React/JavaScript artifacts will not be built from sbt and must be build externally, e.g. via yarn. BUILD_REACT_EXTERNALLY is set to true. Unset or set to != true in order to build it from sbt again.")
  }
  result
}

// Additional compiler (javac, scalac) parameters
val compilerParams: (Seq[String], Seq[String]) = if(System.getProperty("java.version").split("\\.").head.toInt >= 17) {
  (Seq("--release", "17", "-Xlint"), Seq("-release", "17"))
} else {
  (Seq("--release", "11", "-Xlint"), Seq("-release", "11"))
}

(Global / concurrentRestrictions) += Tags.limit(Tags.Test, 1)

val scalaTestOptions = {
  if(sys.env.getOrElse("BUILD_ENV", "develop").toLowerCase == "production") {
    "-oDW"
  } else {
    "-oD"
  }
}

lazy val commonSettings = Seq(
  organization := "org.silkframework",
  version := {
    if(SilkBuildHelpers.isSnapshotVersion(silkVersion)) {
      NEXT_VERSION + "-SNAPSHOT"
    } else {
      silkVersion
    }
  },
  // Building
  scalaVersion := "2.13.12",
  publishTo := {
    val artifactory = "https://artifactory.eccenca.com/"
    // Assumes that version strings for releases, e.g. v3.0.0 or v3.0.0-rc3, do not have a postfix of length 5 or longer.
    // Length 5 was chosen as lower limit because of the "dirty" postfix. Note that isSnapshot does not do the right thing here.
    if (SilkBuildHelpers.isSnapshotVersion(silkVersion)) {
      Some("snapshots" at artifactory + "maven-ecc-snapshot")
    } else {
      Some("releases" at artifactory + "maven-ecc-release")
    }
  },
  // If SBT_PUBLISH_TESTS_JARS ENV variable is set to "true" then tests jar files will be published that can be used e.g. in testing plugins
  (Test / packageBin / publishArtifact) := sys.env.getOrElse("SBT_PUBLISH_TESTS_JARS", "false").toLowerCase == "true",
  (Test / packageSrc / publishArtifact) := sys.env.getOrElse("SBT_PUBLISH_TESTS_JARS", "false").toLowerCase == "true",
  // Testing
  libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.16" % "test",
  libraryDependencies += "net.codingwell" %% "scala-guice" % "6.0.0" % "test",
  libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.4.14",
  libraryDependencies += "org.mockito" % "mockito-core" % "5.3.1" % Test,
  libraryDependencies += "com.google.inject" % "guice" % "5.1.0" % "test",
  libraryDependencies += "javax.inject" % "javax.inject" % "1",
  (Test / testOptions) += Tests.Argument(TestFrameworks.ScalaTest, "-u", "target/test-reports", scalaTestOptions),

  // We need to overwrite the versions of the Jackson modules. We might be able to remove this after a Play upgrade
  dependencyOverrides += "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.14.2",
  dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind" % "2.14.2",

  scalacOptions ++= compilerParams._2,
  javacOptions ++= compilerParams._1,

  Test / javaOptions ++= Seq(
    // Needed by Play 2.8.x for JDK 17 support
    "--add-exports=java.base/sun.security.x509=ALL-UNNAMED",
    "--add-opens=java.base/sun.security.ssl=ALL-UNNAMED",
    // Needed by ldmb for JDK 17 support
    "--add-opens=java.base/java.nio=ALL-UNNAMED",
    "--add-exports=java.base/sun.nio.ch=ALL-UNNAMED"
  )
)

//////////////////////////////////////////////////////////////////////////////
// Core Modules
//////////////////////////////////////////////////////////////////////////////

lazy val core = (project in file("silk-core"))
  .settings(commonSettings: _*)
  .settings(
    name := "Silk Core",
    libraryDependencies += "com.typesafe" % "config" % "1.4.2", // Should always use the same version as the Play Framework dependency
    // Additional scala standard libraries
    libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "2.2.0",
    libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value,
    libraryDependencies += "org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.4",
    libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2",
    libraryDependencies += "commons-io" % "commons-io" % "2.15.0",
    libraryDependencies += "org.lz4" % "lz4-java" % "1.8.0",
    libraryDependencies += "javax.xml.bind" % "jaxb-api" % "2.3.1",
    libraryDependencies += "xalan" % "xalan" % "2.7.3",
    libraryDependencies += "xalan" % "serializer" % "2.7.3",
    libraryDependencies += "io.micrometer" % "micrometer-registry-prometheus" % "1.12.3"
  )

lazy val rules = (project in file("silk-rules"))
  .dependsOn(core % "test->test;compile->compile", pluginsCsv % "test->compile", pluginsJson % "test->compile")
  .settings(commonSettings: _*)
  .settings(
    name := "Silk Rules",
    libraryDependencies += "org.postgresql" % "postgresql" % "42.7.3",
    libraryDependencies += "org.apache.jena" % "jena-core" % "5.0.0" exclude("org.slf4j", "slf4j-log4j12"),
    libraryDependencies += "org.apache.jena" % "jena-arq" % "5.0.0" exclude("org.slf4j", "slf4j-log4j12")
  )

lazy val workspace = (project in file("silk-workspace"))
  .dependsOn(rules, core % "test->test", pluginsJson % "test->compile;test->test", pluginsCsv % "test->compile")
  .aggregate(rules)
  .settings(commonSettings: _*)
  .settings(
    name := "Silk Workspace",
    libraryDependencies += ws
  )

/////////////////////////////////////////////// ///////////////////////////////
// Plugins
//////////////////////////////////////////////////////////////////////////////

lazy val pluginsRdf = (project in file("silk-plugins/silk-plugins-rdf"))
  .dependsOn(rules, workspace % "test->test;compile->compile", core % "test->test;compile->compile", pluginsCsv % "test->compile")
  .settings(commonSettings: _*)
  .settings(
    name := "Silk Plugins RDF",
    libraryDependencies += "org.apache.jena" % "jena-fuseki-main" % "5.0.0" % "test",
    libraryDependencies += "org.apache.velocity" % "velocity-engine-core" % "2.3"
)

lazy val pluginsCsv = (project in file("silk-plugins/silk-plugins-csv"))
  .dependsOn(core % "test->test;compile->compile")
  .settings(commonSettings: _*)
  .settings(
    name := "Silk Plugins CSV",
    libraryDependencies += "com.univocity" % "univocity-parsers" % "2.9.1",
    libraryDependencies += "com.github.albfernandez" % "juniversalchardet" % "2.4.0"
  )

lazy val pluginsXml = (project in file("silk-plugins/silk-plugins-xml"))
  .dependsOn(core, workspace % "compile -> compile;test -> test", pluginsRdf % "test->compile", persistentCaching)
  .settings(commonSettings: _*)
  .settings(
    name := "Silk Plugins XML",
    libraryDependencies += "net.sf.saxon" % "Saxon-HE" % "11.5"
  )

lazy val pluginsJson = (project in file("silk-plugins/silk-plugins-json"))
  .dependsOn(core % "compile->compile;test->test", persistentCaching)
  .settings(commonSettings: _*)
  .settings(
    name := "Silk Plugins JSON",
    libraryDependencies += "com.fasterxml.jackson.core" % "jackson-core" % "2.14.2",
    libraryDependencies += "com.typesafe.play" %% "play-json" % "2.10.3"
  )

// pluginsSpatialTemporal has been removed as it uses dependencies from external unreliable repositories
//lazy val pluginsSpatialTemporal = (project in file("silk-plugins/silk-plugins-spatial-temporal"))
//  .dependsOn(rules)
//  .settings(commonSettings: _*)
//  .settings(
//    name := "Silk Plugins SpatialTemporal",
//    libraryDependencies += "com.vividsolutions" % "jts" % "1.13",
//    libraryDependencies += "org.jvnet.ogc" % "ogc-tools-gml-jts" % "1.0.3",
//    libraryDependencies += "org.geotools" % "gt-opengis" % "13.1",
//    libraryDependencies += "org.geotools" % "gt-referencing" % "13.1",
//    libraryDependencies += "org.geotools" % "gt-jts-wrapper" % "13.1",
//    libraryDependencies += "org.geotools" % "gt-epsg-wkt" % "13.1",
//    resolvers += "OpenGeo Maven Repository" at "http://download.osgeo.org/webdav/geotools/"
//  )

lazy val pluginsAsian = (project in file("silk-plugins/silk-plugins-asian"))
  .dependsOn(rules)
  .settings(commonSettings: _*)
  .settings(
    name := "Silk Plugins Asian"
  )

lazy val serializationJson = (project in file("silk-plugins/silk-serialization-json"))
  .dependsOn(core, rules, workspace % "compile -> compile;test -> test")
  .settings(commonSettings: _*)
  .settings(
    name := "Silk Serialization JSON",
    libraryDependencies += "com.typesafe.play" %% "play-json" % "2.10.3",
    libraryDependencies += "io.swagger.core.v3" % "swagger-annotations" % "2.2.20"
  )

lazy val persistentCaching = (project in file("silk-plugins/silk-persistent-caching"))
  .dependsOn(core % "compile -> compile;test -> test")
  .settings(commonSettings: _*)
  .settings(
    name := "Persistent caching",
    libraryDependencies += "org.lmdbjava" % "lmdbjava" % "0.9.0"
  )

// Aggregate all plugins
lazy val plugins = (project in file("silk-plugins"))
  .dependsOn(pluginsRdf, pluginsCsv, pluginsXml, pluginsJson, pluginsAsian, serializationJson, persistentCaching)
  .aggregate(pluginsRdf, pluginsCsv, pluginsXml, pluginsJson, pluginsAsian, serializationJson, persistentCaching)
  .settings(commonSettings: _*)
  .settings(
    name := "Silk Plugins"
  )

//////////////////////////////////////////////////////////////////////////////
// Silk React Workbench
//////////////////////////////////////////////////////////////////////////////

/** Directories of the legacy UI code. */
val silkWorkbenchRoot: Def.Initialize[File] = Def.setting {
  new File(baseDirectory.value, "../silk-workbench")
}

val silkDistRoot: Def.Initialize[File] = Def.setting {
  new File(baseDirectory.value, "../silk-workbench/silk-workbench-core/public/libs/silk-legacy-ui")
}

val silkLegacyUiRoot: Def.Initialize[File] = Def.setting {
  new File(baseDirectory.value, "../silk-legacy-ui")
}

val checkJsBuildTools = taskKey[Unit]("Check the commandline tools yarn")
val buildDiReact = taskKey[Unit]("Builds Workbench React module")
val yarnInstall = taskKey[Unit]("Runs yarn install.")
val generateLanguageFiles = taskKey[Unit]("Generate i18n language files.")

lazy val reactUI = (project in file("workspace"))
  .settings(commonSettings: _*)
  .settings(
    name := "Workspace React UI",
    //////////////////////////////////////////////////////////////////////////////
    // React UI pipeline
    //////////////////////////////////////////////////////////////////////////////
    /** Check that all necessary build tool for the JS pipeline are available */
    checkJsBuildTools := Def.taskDyn {
      if(!buildReactExternally) {
        Def.task { ReactBuildHelper.checkReactBuildTool() }
      } else {
        Def.task { }
      }
    }.value,
    yarnInstall := Def.taskDyn {
      checkJsBuildTools.value
      if(!buildReactExternally) {
        Def.task { ReactBuildHelper.yarnInstall(baseDirectory.value) }
      } else {
        Def.task { }
      }
    }.value,
    generateLanguageFiles := Def.taskDyn {
      yarnInstall.value
      if(!buildReactExternally) {
        Def.task[Unit] {
          val reactWatchConfig = WatchConfig(new File(baseDirectory.value, "src/locales/manual"), fileRegex = """\.json$""")
          if(Watcher.staleTargetFiles(reactWatchConfig, Seq(new File(baseDirectory.value, "src/locales/generated/en.json")))) {
            ReactBuildHelper.log.info("Generating i18n language files in 'src/locales/generated'...")
            ReactBuildHelper.process(Seq(ReactBuildHelper.yarnCommand, "i18n-parser"), baseDirectory.value)
          }
        }
      } else {
        Def.task { }
      }
    }.value,
    /** Build DataIntegration React */
    buildDiReact := {
      generateLanguageFiles.value
      val workbenchRoot = silkWorkbenchRoot.value
      val legacyRoot = silkLegacyUiRoot.value
      if(!buildReactExternally) {
        // TODO: Add additional source directories
        val reactWatchConfig = WatchConfig(new File(baseDirectory.value, "src"), fileRegex = """\.(tsx|ts|scss|json)$""")
        def distFile(name: String): File = {
          val buildConfig = ReactBuildHelper.buildConfig(baseDirectory.value)
          val distDirectoryRelative = if(buildConfig.containsKey("appDIBuild")) {
            buildConfig.getProperty("appDIBuild")
          } else {
            "../silk-workbench/public"
          }
          val distDirectory = new File(baseDirectory.value, distDirectoryRelative)
          new File(distDirectory, name)
        }
        if (Watcher.staleTargetFiles(reactWatchConfig, Seq(distFile("index.html")))) {
          ReactBuildHelper.buildReactComponents(baseDirectory.value, "Workbench")
        }

        /** Transpile pure JavaScript files of the legacy UI */
        val silkLegacyWorkbenchRoot = new File(legacyRoot, "silk-workbench")
        val changedJsFiles = Watcher.filesChanged(WatchConfig(silkLegacyWorkbenchRoot, fileRegex = """\.js$"""))

        if(changedJsFiles.nonEmpty) {
          // Transpile JavaScript files to ES5
          for(file <- changedJsFiles) {
            val relativePath = silkLegacyWorkbenchRoot.toURI().relativize(file.toURI()).getPath()
            val targetFile = new File(workbenchRoot, relativePath)
            ReactBuildHelper.transpileJavaScript(baseDirectory.value, file, targetFile)
          }
        }
      }
    },
    (Compile / compile) := ((Compile / compile) dependsOn buildDiReact).value,
    watchSources ++= { // Watch all files under the workspace/src directory for changes
      if(buildReactExternally) {
        Seq.empty // Do not watch sources if the workspace is built externally
      } else {
        val paths = for(path <- Path.allSubpaths(baseDirectory.value / "src")) yield {
          path._1
        }
        paths.toSeq ++ Seq(
          new File(baseDirectory.value, "src/locales/manual/en.json"),
          new File(baseDirectory.value, "src/locales/generated/en.json")
        )
      }
    },
    watchSources ++= { // Watch all JavaScript files under the silk-legacy-ui/silk-workbench directory for changes
      val paths = for(path <- Path.allSubpaths(silkLegacyUiRoot.value / "silk-workbench")) yield {
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
  .dependsOn(workspace, workspace % "test -> test", core % "test->test", serializationJson, pluginsXml % "test->compile", pluginsRdf % "test->compile", reactUI)
  .aggregate(workspace, reactUI)
  .settings(commonSettings: _*)
  .settings(
    name := "Silk Workbench Core",
    // Play filters (CORS filter etc.)
    libraryDependencies += filters,
    libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "6.0.1" % "test"
  )

lazy val workbenchWorkspace = (project in file("silk-workbench/silk-workbench-workspace"))
  .enablePlugins(PlayScala)
  .dependsOn(workbenchCore % "compile->compile;test->test", pluginsRdf, pluginsCsv % "test->compile", pluginsXml % "test->compile")
  .aggregate(workbenchCore)
  .settings(commonSettings: _*)
  .settings(
    name := "Silk Workbench Workspace",
    libraryDependencies += ws % "test"
  )

lazy val workbenchRules = (project in file("silk-workbench/silk-workbench-rules"))
  .enablePlugins(PlayScala)
  .dependsOn(workbenchWorkspace % "compile->compile;test->test", pluginsXml % "test->compile", pluginsJson % "test->compile")
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

lazy val workbenchOpenApi = (project in file("silk-workbench/silk-workbench-openapi"))
  .enablePlugins(PlayScala)
  .dependsOn(workbenchCore)
  .settings(commonSettings: _*)
  .settings(
    name := "Silk Workbench OpenAPI",
    libraryDependencies += "io.kinoplan" %% "swagger-play" % "0.0.5" exclude("org.scala-lang.modules", "scala-java8-compat_2.13") ,
    libraryDependencies += "io.swagger.parser.v3" % "swagger-parser-v3" % "2.1.20",
    libraryDependencies += "com.networknt" % "json-schema-validator" % "1.0.78",
    libraryDependencies += "org.webjars" % "swagger-ui" % "5.11.8"
  )

lazy val workbench = (project in file("silk-workbench"))
    .enablePlugins(PlayScala)
    .dependsOn(workbenchWorkspace % "compile->compile;test->test", workbenchRules, workbenchWorkflow, workbenchOpenApi, plugins)
    .aggregate(workbenchWorkspace, workbenchRules, workbenchWorkflow, workbenchOpenApi, plugins)
    .settings(commonSettings: _*)
    .settings(
      name := "Silk Workbench",
      libraryDependencies += guice,
      // Linux Packaging, Uncomment to generate Debian packages that register the Workbench as an Upstart service
      // packageArchetype.java_server
      (Debian / version) := "2.7.2",
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
    libraryDependencies += "org.slf4j" % "slf4j-jdk14" % "2.0.5"
  )

//lazy val mapreduce = (project in file("silk-tools/silk-mapreduce"))
//  .dependsOn(core, plugins)
//  .aggregate(core, plugins)
//  .settings(commonSettings: _*)
//  .settings(
//    name := "Silk MapReduce"
//  )

//////////////////////////////////////////////////////////////////////////////
// Root
//////////////////////////////////////////////////////////////////////////////

lazy val root = (project in file("."))
  .aggregate(core, plugins, singlemachine, workspace, workbench)
  .settings(commonSettings: _*)
