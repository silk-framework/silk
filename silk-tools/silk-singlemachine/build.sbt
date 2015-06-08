name := "Silk SingleMachine"

version := "2.6.1-SNAPSHOT"

scalaVersion := "2.11.6"

assemblyExcludedJars in assembly <<= (fullClasspath in assembly) map { cp =>
  cp filter {x => x.data.getName.matches("sbt.*") || x.data.getName.matches(".*macros.*")}
}