name := "Silk Core"

version := "2.6.1-SNAPSHOT"

libraryDependencies += "com.rockymadden.stringmetric" % "stringmetric-core_2.10" % "0.27.2"

libraryDependencies += "com.thoughtworks.paranamer" % "paranamer" % "2.6"

libraryDependencies += "org.clapper" % "classutil_2.10" % "1.0.5"

libraryDependencies += "com.vividsolutions" % "jts" % "1.13"

libraryDependencies += "org.jvnet.ogc" % "ogc-tools-gml-jts" % "1.0.2"

libraryDependencies += "org.geotools" % "gt-opengis" % "2.7.4"

libraryDependencies += "org.geotools" % "gt-referencing" % "2.7.4"

libraryDependencies += "org.geotools" % "gt-jts-wrapper" % "2.7.4"
    
libraryDependencies += "org.geotools" % "gt-epsg-wkt" % "2.7.4"


resolvers += "OpenGeo Maven Repository" at "http://download.osgeo.org/webdav/geotools/"
