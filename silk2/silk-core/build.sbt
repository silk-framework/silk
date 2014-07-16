name := "Silk Core"

version := "2.6.1-SNAPSHOT"

scalaVersion := "2.11.1"

libraryDependencies += "com.rockymadden.stringmetric" % "stringmetric-core_2.11" % "0.27.4"

libraryDependencies += "com.thoughtworks.paranamer" % "paranamer" % "2.6"

libraryDependencies += "org.clapper" % "classutil_2.11" % "1.0.5"

// Testing dependencies

testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-u", "target/test-reports")

libraryDependencies += "junit" % "junit" % "4.11" % "test"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.1.6" % "test"

// Additional scala standard libraries

libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.2"

libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.1"


//Spatial Extensions

libraryDependencies += "com.vividsolutions" % "jts" % "1.13"

libraryDependencies += "org.jvnet.ogc" % "ogc-tools-gml-jts" % "1.0.2"

libraryDependencies += "org.geotools" % "gt-opengis" % "2.7.4"

libraryDependencies += "org.geotools" % "gt-referencing" % "2.7.4"

libraryDependencies += "org.geotools" % "gt-jts-wrapper" % "2.7.4"
    
libraryDependencies += "org.geotools" % "gt-epsg-wkt" % "2.7.4"


resolvers += "OpenGeo Maven Repository" at "http://download.osgeo.org/webdav/geotools/"

