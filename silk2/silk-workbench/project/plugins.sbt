// Comment to get more information during initialization
logLevel := Level.Warn

// The Typesafe repository 
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.2.1")

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.2.1")

// Uncomment to use a more recent version of the SBT Native Packager than is included in Play
// addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "0.7.0-M1")

// Plugin for generating WAR files.
addSbtPlugin("com.github.play2war" % "play2-war-plugin" % "1.2-beta2")