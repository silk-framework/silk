resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.4.8")

// Plugin for generating WAR files.
addSbtPlugin("com.github.play2war" % "play2-war-plugin" % "1.4-beta1")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.0")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.8.1")

addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.8.5")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.5.0")