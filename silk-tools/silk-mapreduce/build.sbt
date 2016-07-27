name := "Silk MapReduce"

version := "2.6.1-SNAPSHOT"

scalaVersion := "2.11.6"

resolvers += "cloudera" at "https://repository.cloudera.com/artifactory/cloudera-repos/"

libraryDependencies += "org.apache.hadoop" % "hadoop-common" % "2.6.0-cdh5.7.0"
libraryDependencies += "org.apache.hadoop" % "hadoop-client" % "2.6.0-cdh5.7.0"
