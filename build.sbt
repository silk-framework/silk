import sbt.Keys.libraryDependencies
import sbt.{File, taskKey, *}

import java.io.FileWriter

//////////////////////////////////////////////////////////////////////////////
// Common Settings
//////////////////////////////////////////////////////////////////////////////

val NEXT_VERSION = "v26.2.0"
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
// Note: keep the bytecode target at 21, the same as in the root build (needed for Spark)
val compilerParams: (Seq[String], Seq[String]) =
  (Seq("--release", "21", "-Xlint"), Seq("-release", "21"))

// One version for every jena module, must match the version in the root build.
val jenaVersion = "6.2.0"

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
  scalaVersion := "2.13.18",
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
  libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.20" % "test",
  libraryDependencies += "net.codingwell" %% "scala-guice" % "6.0.0" % "test",
  libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.6.6",
  libraryDependencies += "org.mockito" % "mockito-core" % "5.24.0" % Test,
  libraryDependencies += "com.google.inject" % "guice" % "6.0.0" % "test",
  libraryDependencies += "javax.inject" % "javax.inject" % "1",
  // org.lz4:lz4-java is abandoned. silk-core brings the maintained fork at.yawk.lz4, which has the same classes.
  excludeDependencies += "org.lz4" % "lz4-java",
  (Test / testOptions) += Tests.Argument(TestFrameworks.ScalaTest, "-u", "target/test-reports", scalaTestOptions),

  // We need to overwrite the versions of the Jackson modules.
  // Fixes GHSA-72hv-8253-57qq and CVE-2025-52999 (require 2.21.1+) and
  // CVE-2026-54512/54515/54516/54517, which still affect 2.22.0 (require 2.22.1).
  dependencyOverrides += "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.22.4",
  dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind" % "2.22.4",
  dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-core" % "2.22.4",

  // Jackson 3.x (json-schema-validator): CVE-2026-89425 and CVE-2026-91776/91777 require 3.2.3.
  dependencyOverrides += "tools.jackson.core" % "jackson-core" % "3.2.4",
  dependencyOverrides += "tools.jackson.core" % "jackson-databind" % "3.2.4",

  // Fix CVE-2026-8763 and CVE-2026-13506 in Bouncy Castle (require 1.85); only on the pluginsRdf test classpath.
  dependencyOverrides += "org.bouncycastle" % "bcprov-jdk18on" % "1.86",

  // Fix CVE-2026-102554 in Guava (requires 33.7.2).
  dependencyOverrides += "com.google.guava" % "guava" % "33.8.0-jre",

  // silk-core uses scala-parser-combinators 2.x (required by Spark 4 in the root build); it evicts the 1.1.2 of Play 2.9.
  libraryDependencySchemes += "org.scala-lang.modules" %% "scala-parser-combinators" % VersionScheme.Always,

  scalacOptions ++= compilerParams._2,
  javacOptions ++= compilerParams._1,

  Test / javaOptions ++= Seq(
    // Needed by lmdb for JDK 17+ support
    "--add-opens=java.base/java.nio=ALL-UNNAMED",
    "--add-exports=java.base/sun.nio.ch=ALL-UNNAMED"
  )
)

//////////////////////////////////////////////////////////////////////////////
// Core Modules
//////////////////////////////////////////////////////////////////////////////

lazy val core = (project in file("silk-core"))
  .settings(commonSettings *)
  .settings(
    name := "Silk Core",
    libraryDependencies += "com.typesafe" % "config" % "1.4.9",
    // Additional scala standard libraries
    libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "2.5.0",
    libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value,
    libraryDependencies += "org.scala-lang.modules" %% "scala-parallel-collections" % "1.3.0",
    libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "2.5.0",
    libraryDependencies += "commons-io" % "commons-io" % "2.22.0",
    libraryDependencies += "at.yawk.lz4" % "lz4-java" % "1.13.0",
    libraryDependencies += "javax.xml.bind" % "jaxb-api" % "2.3.1",
    libraryDependencies += "xalan" % "xalan" % "2.7.3",
    libraryDependencies += "xalan" % "serializer" % "2.7.3",
    libraryDependencies += "org.snakeyaml" % "snakeyaml-engine" % "3.2",
    libraryDependencies += "io.micrometer" % "micrometer-registry-prometheus" % "1.17.2"
  )

lazy val rules = (project in file("silk-rules"))
  .dependsOn(core % "test->test;compile->compile", pluginsCsv % "test->compile", pluginsJson % "test->compile")
  .settings(commonSettings *)
  .settings(
    name := "Silk Rules",
    libraryDependencies += "org.postgresql" % "postgresql" % "42.7.14",
    libraryDependencies += "org.apache.jena" % "jena-core" % jenaVersion exclude("org.slf4j", "slf4j-log4j12"),
    libraryDependencies += "org.apache.jena" % "jena-arq" % jenaVersion exclude("org.slf4j", "slf4j-log4j12")
  )

lazy val workspace = (project in file("silk-workspace"))
  .dependsOn(rules, core % "test->test", pluginsJson % "test->compile;test->test", pluginsCsv % "test->compile")
  .aggregate(rules)
  .settings(commonSettings *)
  .settings(
    name := "Silk Workspace",
    libraryDependencies += ws
  )

/////////////////////////////////////////////// ///////////////////////////////
// Plugins
//////////////////////////////////////////////////////////////////////////////

lazy val pluginsTemplatingJinja = (project in file("silk-plugins/silk-plugins-templating-jinja"))
  .dependsOn(rules % "compile->compile;test->test")
  .settings(commonSettings *)
  .settings(
    name := "Silk Plugins Templating Jinja",
    libraryDependencies += "com.hubspot.jinjava" % "jinjava" % "2.8.5"
  )

lazy val pluginsTemplatingVelocity = (project in file("silk-plugins/silk-plugins-templating-velocity"))
  .dependsOn(rules % "compile->compile;test->test")
  .settings(commonSettings *)
  .settings(
    name := "Silk Plugins Templating Velocity",
    libraryDependencies += "org.apache.velocity" % "velocity-engine-core" % "2.4.1"
  )

lazy val pluginsRdf = (project in file("silk-plugins/silk-plugins-rdf"))
  .dependsOn(rules, workspace % "test->test;compile->compile", core % "test->test;compile->compile", pluginsCsv % "test->compile",
             pluginsTemplatingJinja % "test->compile", pluginsTemplatingVelocity % "test->compile")
  .enablePlugins(JmhPlugin)
  .settings(commonSettings *)
  .settings(
    name := "Silk Plugins RDF",
    libraryDependencies += "org.apache.jena" % "jena-fuseki-main" % jenaVersion % "test"
)

lazy val pluginsCsv = (project in file("silk-plugins/silk-plugins-csv"))
  .dependsOn(core % "test->test;compile->compile")
  .settings(commonSettings *)
  .settings(
    name := "Silk Plugins CSV",
    libraryDependencies += "com.univocity" % "univocity-parsers" % "2.9.1",
    libraryDependencies += "com.github.albfernandez" % "juniversalchardet" % "2.5.2"
  )

lazy val pluginsXml = (project in file("silk-plugins/silk-plugins-xml"))
  .dependsOn(core, workspace % "compile -> compile;test -> test", pluginsRdf % "test->compile", persistentCaching)
  .settings(commonSettings *)
  .settings(
    name := "Silk Plugins XML",
    libraryDependencies += "net.sf.saxon" % "Saxon-HE" % "13.1",
    // Explicit StAX implementation, since XmlSourceStreaming turns parser positions into entity URIs
    libraryDependencies += "com.fasterxml.woodstox" % "woodstox-core" % "7.3.0"
  )

lazy val pluginsJson = (project in file("silk-plugins/silk-plugins-json"))
  .dependsOn(core % "compile->compile;test->test", persistentCaching)
  .settings(commonSettings *)
  .settings(
    name := "Silk Plugins JSON",
    libraryDependencies += "com.typesafe.play" %% "play-json" % "2.10.8"
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
  .settings(commonSettings *)
  .settings(
    name := "Silk Plugins Asian"
  )

lazy val serializationJson = (project in file("silk-plugins/silk-serialization-json"))
  .dependsOn(core, rules, workspace % "compile -> compile;test -> test")
  .settings(commonSettings *)
  .settings(
    name := "Silk Serialization JSON",
    libraryDependencies += "com.typesafe.play" %% "play-json" % "2.10.8",
    libraryDependencies += "io.swagger.core.v3" % "swagger-annotations" % "2.2.55"
  )

lazy val persistentCaching = (project in file("silk-plugins/silk-persistent-caching"))
  .dependsOn(core % "compile -> compile;test -> test")
  .settings(commonSettings *)
  .settings(
    name := "Persistent caching",
    libraryDependencies += "org.lmdbjava" % "lmdbjava" % "0.9.3"
  )

// Aggregate all plugins
lazy val plugins = (project in file("silk-plugins"))
  .dependsOn(pluginsRdf, pluginsCsv, pluginsXml, pluginsJson, pluginsAsian, serializationJson, persistentCaching, pluginsTemplatingJinja, pluginsTemplatingVelocity)
  .aggregate(pluginsRdf, pluginsCsv, pluginsXml, pluginsJson, pluginsAsian, serializationJson, persistentCaching, pluginsTemplatingJinja, pluginsTemplatingVelocity)
  .settings(commonSettings *)
  .settings(
    name := "Silk Plugins"
  )

//////////////////////////////////////////////////////////////////////////////
// Silk React Workbench
//////////////////////////////////////////////////////////////////////////////

val checkJsBuildTools = taskKey[Unit]("Check the commandline tools yarn")
val buildDiReact = taskKey[Unit]("Builds Workbench React module")
val yarnInstall = taskKey[Unit]("Runs yarn install.")
val generateLanguageFiles = taskKey[Unit]("Generate i18n language files.")

lazy val reactUI = (project in file("workspace"))
  .settings(commonSettings *)
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
    }
  )

//////////////////////////////////////////////////////////////////////////////
// Workbench
//////////////////////////////////////////////////////////////////////////////

lazy val workbenchCore = (project in file("silk-workbench/silk-workbench-core"))
  .enablePlugins(PlayScala)
  .dependsOn(workspace, workspace % "test -> test", core % "test->test", serializationJson, pluginsXml % "test->compile", pluginsRdf % "test->compile", reactUI)
  .aggregate(workspace, reactUI)
  .settings(commonSettings *)
  .settings(
    name := "Silk Workbench Core",
    // Play filters (CORS filter etc.)
    libraryDependencies += filters,
    libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "6.0.2" % "test",
    libraryDependencies += "io.micrometer" % "micrometer-registry-prometheus" % "1.17.2"
  )

lazy val workbenchWorkspace = (project in file("silk-workbench/silk-workbench-workspace"))
  .enablePlugins(PlayScala)
  .dependsOn(workbenchCore % "compile->compile;test->test", pluginsRdf, pluginsCsv % "test->compile", pluginsXml % "test->compile", pluginsTemplatingJinja % "test->compile")
  .aggregate(workbenchCore)
  .settings(commonSettings *)
  .settings(
    name := "Silk Workbench Workspace",
    libraryDependencies += ws % "test"
  )

lazy val workbenchRules = (project in file("silk-workbench/silk-workbench-rules"))
  .enablePlugins(PlayScala)
  .dependsOn(workbenchWorkspace % "compile->compile;test->test", pluginsXml % "test->compile", pluginsJson % "test->compile")
  .aggregate(workbenchWorkspace)
  .settings(commonSettings *)
  .settings(
    name := "Silk Workbench Rules"
  )

lazy val workbenchWorkflow = (project in file("silk-workbench/silk-workbench-workflow"))
  .enablePlugins(PlayScala)
  .dependsOn(workbenchWorkspace % "compile->compile;test->test", workbenchRules, serializationJson)
  .aggregate(workbenchWorkspace)
  .settings(commonSettings *)
  .settings(
    name := "Silk Workbench Workflow"
  )

lazy val workbenchOpenApi = (project in file("silk-workbench/silk-workbench-openapi"))
  .enablePlugins(PlayScala)
  .dependsOn(workbenchCore)
  .settings(commonSettings *)
  .settings(
    name := "Silk Workbench OpenAPI",
    // swagger-play 0.0.7 is built for Play 3. Keep its Play 3 and Pekko dependencies off the Play 2.9 classpath
    // and give it the routes compiler of our Play version instead.
    libraryDependencies += ("io.kinoplan" %% "swagger-play" % "0.0.7")
      .exclude("org.scala-lang.modules", "scala-java8-compat_2.13")
      .excludeAll(ExclusionRule("org.playframework"), ExclusionRule("org.playframework.twirl"), ExclusionRule("org.apache.pekko")),
    libraryDependencies += "com.typesafe.play" %% "play-routes-compiler" % play.core.PlayVersion.current,
    libraryDependencies += "io.swagger.parser.v3" % "swagger-parser-v3" % "2.1.48",
    libraryDependencies += "com.networknt" % "json-schema-validator" % "3.0.9",
    libraryDependencies += "org.webjars" % "swagger-ui" % "5.34.1"
  )

lazy val workbench = (project in file("silk-workbench"))
    .enablePlugins(PlayScala)
    .dependsOn(workbenchWorkspace % "compile->compile;test->test", workbenchRules, workbenchWorkflow, workbenchOpenApi, plugins)
    .aggregate(workbenchWorkspace, workbenchRules, workbenchWorkflow, workbenchOpenApi, plugins)
    .settings(commonSettings *)
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
  .settings(commonSettings *)
  .settings(
    name := "Silk SingleMachine",
    libraryDependencies += "org.slf4j" % "slf4j-jdk14" % "2.0.20"
  )

//////////////////////////////////////////////////////////////////////////////
// Root
//////////////////////////////////////////////////////////////////////////////

lazy val root = (project in file("."))
  .aggregate(core, plugins, singlemachine, workspace, workbench)
  .settings(commonSettings *)
