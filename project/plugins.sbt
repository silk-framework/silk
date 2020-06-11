resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.6.23")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.10")

// Not yet supported with sbt 1.3.x
// addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.8.1")
