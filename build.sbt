import sbt.{File, taskKey, _}

import java.io._

//////////////////////////////////////////////////////////////////////////////
// Common Settings
//////////////////////////////////////////////////////////////////////////////

val NEXT_VERSION = "3.1.0"
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

val compilerParams: (Seq[String], Seq[String]) = if(System.getProperty("java.version").split("\\.").head.toInt > 8) {
  (Seq("--release", "11", "-Xlint"), Seq("-release", "11"))
} else {
  (Seq("-source", "1.8", "-target", "1.8", "-Xlint"), Seq.empty)
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
  scalaVersion := "2.12.15",
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
  libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.9" % "test",
  libraryDependencies += "net.codingwell" %% "scala-guice" % "4.2.11" % "test",
  libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.11",
  libraryDependencies += "org.mockito" % "mockito-all" % "1.9.5" % "test",
  libraryDependencies += "com.google.inject" % "guice" % "4.0" % "test",
  libraryDependencies += "javax.inject" % "javax.inject" % "1",
  (Test / testOptions) += Tests.Argument(TestFrameworks.ScalaTest, "-u", "target/test-reports", scalaTestOptions),

  // We need to overwrite the versions of the Jackson modules. We might be able to remove this after a Play upgrade
  dependencyOverrides += "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.12.6" % "test",
  dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind" % "2.12.6" % "test",
  // We need to make sure that no newer versions of slf4j are used because logback 1.2.x only supports slf4j up to 1.7.x
  // Can be removed as soon as there are newer stable versions of logback
  dependencyOverrides += "org.slf4j" % "slf4j-api" % "1.7.36",

  // The assembly plugin cannot resolve multiple dependencies to commons logging
  (assembly / assemblyMergeStrategy) := {
    case PathList("org", "apache", "commons", "logging",  xs @ _*) => MergeStrategy.first
    case PathList(xs @ _*) if xs.last endsWith ".class" => MergeStrategy.first
    case PathList(xs @ _*) if xs.last endsWith ".xsd" => MergeStrategy.first
    case PathList(xs @ _*) if xs.last endsWith ".dtd" => MergeStrategy.first
    case other =>
      val oldStrategy = (assembly / assemblyMergeStrategy).value
      oldStrategy(other)
  },
  scalacOptions ++= compilerParams._2,
  javacOptions ++= compilerParams._1,
)

//////////////////////////////////////////////////////////////////////////////
// Core Modules
//////////////////////////////////////////////////////////////////////////////

lazy val core = (project in file("silk-core"))
  .settings(commonSettings: _*)
  .settings(
    name := "Silk Core",
    libraryDependencies += "com.typesafe" % "config" % "1.4.1", // Should always use the same version as the Play Framework dependency
    libraryDependencies += "com.github.halfmatthalfcat" %% "stringmetric-core" % "0.28.0",
    // Additional scala standard libraries
    libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.1.0",
    libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value,
    libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.1",
    libraryDependencies += "commons-io" % "commons-io" % "2.4",
    libraryDependencies += "org.lz4" % "lz4-java" % "1.4.0",
    libraryDependencies += "javax.xml.bind" % "jaxb-api" % "2.3.1",
    libraryDependencies += "xalan" % "xalan" % "2.7.2"
  )

lazy val rules = (project in file("silk-rules"))
  .dependsOn(core % "test->test;compile->compile", pluginsCsv % "test->compile")
  .settings(commonSettings: _*)
  .settings(
    name := "Silk Rules",
    libraryDependencies += "org.postgresql" % "postgresql" % "42.2.5",
    libraryDependencies += "org.apache.jena" % "jena-core" % "4.4.0" exclude("org.slf4j", "slf4j-log4j12"),
    libraryDependencies += "org.apache.jena" % "jena-arq" % "4.4.0" exclude("org.slf4j", "slf4j-log4j12")
  )

lazy val learning = (project in file("silk-learning"))
  .dependsOn(rules, workspace)
  .settings(commonSettings: _*)
  .settings(
    name := "Silk Learning"
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
    libraryDependencies += "org.apache.jena" % "jena-fuseki-main" % "4.4.0" % "test",
    libraryDependencies += "org.apache.velocity" % "velocity-engine-core" % "2.1"
)

lazy val pluginsCsv = (project in file("silk-plugins/silk-plugins-csv"))
  .dependsOn(core)
  .settings(commonSettings: _*)
  .settings(
    name := "Silk Plugins CSV",
    libraryDependencies += "com.univocity" % "univocity-parsers" % "2.8.3"
  )

lazy val pluginsXml = (project in file("silk-plugins/silk-plugins-xml"))
  .dependsOn(core, workspace % "compile -> compile;test -> test", pluginsRdf % "test->compile", persistentCaching)
  .settings(commonSettings: _*)
  .settings(
    name := "Silk Plugins XML",
    libraryDependencies += "net.sf.saxon" % "Saxon-HE" % "9.8.0-6"
  )

lazy val pluginsJson = (project in file("silk-plugins/silk-plugins-json"))
  .dependsOn(core % "compile->compile;test->test", persistentCaching)
  .settings(commonSettings: _*)
  .settings(
    name := "Silk Plugins JSON",
    libraryDependencies += "com.fasterxml.jackson.core" % "jackson-core" % "2.12.1",
    libraryDependencies += "com.typesafe.play" %% "play-json" % "2.8.2"
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
  .dependsOn(core, rules, workspace)
  .settings(commonSettings: _*)
  .settings(
    name := "Silk Serialization JSON",
    libraryDependencies += "com.typesafe.play" %% "play-json" % "2.8.2",
    libraryDependencies += "io.swagger.core.v3" % "swagger-annotations" % "2.2.0"
  )

lazy val persistentCaching = (project in file("silk-plugins/silk-persistent-caching"))
  .dependsOn(core)
  .settings(commonSettings: _*)
  .settings(
    name := "Persistent caching",
    libraryDependencies += "org.lmdbjava" % "lmdbjava" % "0.8.2"
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
  .dependsOn(workspace, workspace % "test -> test", core % "test->test", serializationJson, pluginsXml % "test->compile", pluginsRdf % "test->compile")
  .aggregate(workspace)
  .settings(commonSettings: _*)
  .settings(
    name := "Silk Workbench Core",
    // Play filters (CORS filter etc.)
    libraryDependencies += filters,
    libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % "test"
  )

lazy val workbenchWorkspace = (project in file("silk-workbench/silk-workbench-workspace"))
  .enablePlugins(PlayScala)
  .dependsOn(workbenchCore % "compile->compile;test->test", pluginsRdf, pluginsCsv % "test->compile", pluginsXml % "test->compile", reactUI)
  .aggregate(workbenchCore, reactUI)
  .settings(commonSettings: _*)
  .settings(
    name := "Silk Workbench Workspace",
    libraryDependencies += ws % "test"
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

lazy val workbenchOpenApi = (project in file("silk-workbench/silk-workbench-openapi"))
  .enablePlugins(PlayScala)
  .dependsOn(workbenchCore)
  .settings(commonSettings: _*)
  .settings(
    name := "Silk Workbench OpenAPI",
    libraryDependencies += "io.kinoplan" % "swagger-play_2.12" % "0.0.3",
    libraryDependencies += "io.swagger.parser.v3" % "swagger-parser-v3" % "2.0.32",
    libraryDependencies += "com.networknt" % "json-schema-validator" % "1.0.62",
    libraryDependencies += "org.webjars" % "swagger-ui" % "4.10.3"
  )

lazy val workbench = (project in file("silk-workbench"))
    .enablePlugins(PlayScala)
    .dependsOn(workbenchWorkspace, workbenchRules, workbenchWorkflow, workbenchOpenApi, plugins)
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
    libraryDependencies += "org.slf4j" % "slf4j-jdk14" % "1.7.13",
    // The assembly plugin cannot resolve multiple dependencies to commons logging
    (assembly / assemblyMergeStrategy) := {
      case PathList("org", "apache", "commons", "logging",  xs @ _*) => MergeStrategy.first
      case other =>
        val oldStrategy = (assembly / assemblyMergeStrategy).value
        oldStrategy(other)
    }
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
  .aggregate(core, plugins, singlemachine, learning, workspace, workbench)
  .settings(commonSettings: _*)
