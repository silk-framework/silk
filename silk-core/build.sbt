name := "Silk Core"

version := "2.6.1-SNAPSHOT"

scalaVersion := "2.11.6"

libraryDependencies += "com.rockymadden.stringmetric" % "stringmetric-core_2.11" % "0.27.4"

libraryDependencies += "com.thoughtworks.paranamer" % "paranamer" % "2.7"

libraryDependencies += "org.clapper" % "classutil_2.11" % "1.0.5" exclude("org.slf4j", "slf4j-log4j12")

// Testing dependencies

testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-u", "target/test-reports")

libraryDependencies += "junit" % "junit" % "4.11" % "test"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.4" % "test"

// Additional scala standard libraries

libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.4"

libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4"
