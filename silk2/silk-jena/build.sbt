name := "Silk Jena"

version := "2.6.1-SNAPSHOT"

scalaVersion := "2.11.1"

libraryDependencies += "org.apache.jena" % "jena-core" % "2.11.1" exclude("org.slf4j", "slf4j-log4j12")

libraryDependencies += "org.apache.jena" % "jena-arq" % "2.11.1" exclude("org.slf4j", "slf4j-log4j12")