name := "Silk-Workbench"

version := "2.6.0-SNAPSHOT"	

libraryDependencies += "de.fuberlin.wiwiss.silk" % "silk-workspace" % "2.6.0-SNAPSHOT" excludeAll(ExclusionRule(organization = "org.slf4j"))

resolvers += "Local Maven Repository" at "file:"+Path.userHome+"/.m2/repository"

play.Project.playScalaSettings

com.github.play2war.plugin.Play2WarPlugin.play2WarSettings

com.github.play2war.plugin.Play2WarKeys.servletVersion := "3.0"