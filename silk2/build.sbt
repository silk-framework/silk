name := "Silk"

version := "2.6.1-SNAPSHOT"

scalaVersion := "2.10.4"

lazy val core = project in file("silk-core")

lazy val jena = project in file("silk-jena") dependsOn core

lazy val singlemachine = project in file("silk-singlemachine") dependsOn core dependsOn jena

lazy val learning = project in file("silk-learning") dependsOn core

lazy val workspace = project in file("silk-workspace") dependsOn core dependsOn jena dependsOn learning

lazy val workbench = project in file("silk-workbench") dependsOn workspace aggregate workspace enablePlugins PlayScala

lazy val root = project.in(file("."))
                       .aggregate(core, jena, singlemachine, learning, workspace, workbench)

libraryDependencies += "org.scalatest" % "scalatest_2.10" % "2.1.6" % "test"
