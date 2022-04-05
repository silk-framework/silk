// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.8.15")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.10")

// Not yet supported with sbt 1.3.x
// addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.8.1")

libraryDependencies += "org.apache.commons" % "commons-io" % "1.3.2"
