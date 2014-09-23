// General Settings

name := "Silk"

version := "2.6.1-SNAPSHOT"

scalaVersion := "2.11.1"

scalacOptions += "-deprecation"

// Core Modules

lazy val core = project in file("silk-core")

lazy val learning = project in file("silk-learning") dependsOn core

// Plugins

lazy val pluginsJena = project in file("silk-plugins/silk-plugins-jena") dependsOn core

lazy val plugins = project in file("silk-plugins") dependsOn pluginsJena

// Workbench

lazy val workspace = project in file("silk-workspace") dependsOn core dependsOn plugins dependsOn learning

lazy val workbenchCore = project in file("silk-workbench/silk-workbench-core") enablePlugins PlayScala dependsOn workspace aggregate workspace

lazy val workbenchWorkspace = project in file("silk-workbench/silk-workbench-workspace") enablePlugins PlayScala dependsOn workbenchCore aggregate workbenchCore

lazy val workbenchWorkflow = project in file("silk-workbench/silk-workbench-workflow") enablePlugins PlayScala dependsOn workbenchCore aggregate workbenchCore

lazy val workbenchRules = project in file("silk-workbench/silk-workbench-rules") enablePlugins PlayScala dependsOn workbenchCore aggregate workbenchCore

lazy val workbench = project in file("silk-workbench") enablePlugins PlayScala dependsOn (workbenchWorkspace, workbenchRules) aggregate (workbenchWorkspace, workbenchRules)

// Tools

lazy val singlemachine = project in file("silk-tools/silk-singlemachine") dependsOn core dependsOn plugins

// Root

lazy val root = project.in(file("."))
                       .aggregate(core, plugins, pluginsJena, singlemachine, learning, workspace, workbench)

libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.1.6" % "test"

