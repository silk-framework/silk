resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.3.1")

// Plugin for generating WAR files.
addSbtPlugin("com.github.play2war" % "play2-war-plugin" % "1.3-beta1")
