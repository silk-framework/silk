name := "Silk Json Plugins"

version := "2.6.1-SNAPSHOT"

scalaVersion := "2.11.5"

libraryDependencies += "com.typesafe.play" % "play-json_2.11" % "2.3.7"

// Testing dependencies

testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-u", "target/test-reports")

libraryDependencies += "junit" % "junit" % "4.11" % "test"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.1.6" % "test"

// The Typesafe repository
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"