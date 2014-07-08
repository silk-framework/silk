name := "Silk Jena"

version := "2.6.1-SNAPSHOT"

scalaVersion := "2.11.1"

libraryDependencies += "org.apache.jena" % "jena-core" % "2.11.1" exclude("org.slf4j", "slf4j-log4j12")

libraryDependencies += "org.apache.jena" % "jena-arq" % "2.11.1" exclude("org.slf4j", "slf4j-log4j12")

// Testing dependencies

testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-u", "target/test-reports")

libraryDependencies += "junit" % "junit" % "4.11" % "test"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.1.6" % "test"