name := "Silk"

version := "2.6.1-SNAPSHOT"

scalaVersion := "2.11.1"

lazy val core = project in file("silk-core")

lazy val jena = project in file("silk-jena") dependsOn core

lazy val singlemachine = project in file("silk-singlemachine") dependsOn core dependsOn jena

lazy val learning = project in file("silk-learning") dependsOn core

lazy val workspace = project in file("silk-workspace") dependsOn core dependsOn jena dependsOn learning

lazy val workbenchCore = project in file("silk-workbench/silk-workbench-core") enablePlugins PlayScala dependsOn workspace aggregate workspace

lazy val workbenchWorkspace = project in file("silk-workbench/silk-workbench-workspace") enablePlugins PlayScala dependsOn workbenchCore aggregate workbenchCore

lazy val workbenchRules = project in file("silk-workbench/silk-workbench-rules") enablePlugins PlayScala dependsOn workbenchCore aggregate workbenchCore

lazy val workbench = project in file("silk-workbench") enablePlugins PlayScala dependsOn (workbenchWorkspace, workbenchRules) aggregate (workbenchWorkspace, workbenchRules)

lazy val root = project.in(file("."))
                       .aggregate(core, jena, singlemachine, learning, workspace, workbench)

libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.1.6" % "test"
