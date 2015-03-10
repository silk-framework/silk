// Spatial and Temporal Plugins for Silk
// @author Panayiotis Smeros <psmeros@di.uoa.gr> (National and Kapodistrian University of Athens)


name := "Silk Spatial-Temporal Plugins"

version := "2.6.1-SNAPSHOT"

scalaVersion := "2.11.6"


//Testing dependencies

testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-u", "target/test-reports")

libraryDependencies += "junit" % "junit" % "4.11" % "test"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.1.6" % "test"


//Spatial Extensions

libraryDependencies += "com.vividsolutions" % "jts" % "1.13"

libraryDependencies += "org.jvnet.ogc" % "ogc-tools-gml-jts" % "1.0.2"

libraryDependencies += "org.geotools" % "gt-opengis" % "2.7.4"

libraryDependencies += "org.geotools" % "gt-referencing" % "2.7.4"

libraryDependencies += "org.geotools" % "gt-jts-wrapper" % "2.7.4"
    
libraryDependencies += "org.geotools" % "gt-epsg-wkt" % "2.7.4"


resolvers += "OpenGeo Maven Repository" at "http://download.osgeo.org/webdav/geotools/"

