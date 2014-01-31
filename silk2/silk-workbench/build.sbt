////////////////////////////////////////////////
// General Settings
////////////////////////////////////////////////

name := "Silk-Workbench"

version := "2.6.0-SNAPSHOT"	

libraryDependencies += "de.fuberlin.wiwiss.silk" % "silk-workspace" % "2.6.0-SNAPSHOT" excludeAll(ExclusionRule(organization = "org.slf4j"))

resolvers += "Local Maven Repository" at "file:"+Path.userHome+"/.m2/repository"

play.Project.playScalaSettings

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

version in Debian := "2.6.0"

maintainer := "Robert Isele <silk-discussion@googlegroups.com>"

packageSummary := "The Silk framework is a tool for discovering relationships between data items within different Linked Data sources."

packageDescription := "The Silk framework is a tool for discovering relationships between data items within different Linked Data sources."