name := "Silk-Workbench"

version := "2.5.4-SNAPSHOT"	

libraryDependencies += "de.fuberlin.wiwiss.silk" % "silk-workspace" % "2.5.4-SNAPSHOT" excludeAll(ExclusionRule(organization = "org.slf4j"))

resolvers += "Local Maven Repository" at "file:"+Path.userHome+"/.m2/repository"

play.Project.playScalaSettings