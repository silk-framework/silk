import com.typesafe.sbt.packager.Keys._

////////////////////////////////////////////////
// General Settings
////////////////////////////////////////////////

name := "Silk-Workbench"

version := "2.6.1-SNAPSHOT"

scalaVersion := "2.11.6"

////////////////////////////////////////////////
// War Packaging
////////////////////////////////////////////////

com.github.play2war.plugin.Play2WarPlugin.play2WarSettings

com.github.play2war.plugin.Play2WarKeys.servletVersion := "3.0"

////////////////////////////////////////////////
// Linux Packaging
////////////////////////////////////////////////

// Uncomment to generate Debian packages that register the Workbench as an Upstart service
// packageArchetype.java_server

version in Debian := "2.6.1"

maintainer := "Robert Isele <silk-discussion@googlegroups.com>"

packageSummary := "The Silk framework is a tool for discovering relationships between data items within different Linked Data sources."

packageDescription := "The Silk framework is a tool for discovering relationships between data items within different Linked Data sources."