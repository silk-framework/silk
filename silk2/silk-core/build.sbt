name := "Silk Core"

version := "2.6.1-SNAPSHOT"

scalaVersion := "2.11.1"

libraryDependencies += "com.rockymadden.stringmetric" % "stringmetric-core_2.11" % "0.27.4"

libraryDependencies += "com.thoughtworks.paranamer" % "paranamer" % "2.6"

libraryDependencies += "org.clapper" % "classutil_2.11" % "1.0.5"

// Testing dependencies

libraryDependencies += "junit" % "junit" % "4.11" % "test"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.1.6" % "test"

// Additional scala standard libraries

libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.2"

libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.1"
