name := "Silk Jena"

version := "2.6.0-SNAPSHOT"

libraryDependencies += "org.apache.jena" % "jena-core" % "2.11.0" exclude("org.slf4j", "slf4j-log4j12")

libraryDependencies += "org.apache.jena" % "jena-arq" % "2.11.0" exclude("org.slf4j", "slf4j-log4j12")