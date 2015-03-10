name := "Silk Plugins"

version := "2.6.1-SNAPSHOT"

scalaVersion := "2.11.6"

lazy val core = project in file("../silk-core")

lazy val pluginsJena = project in file("silk-plugins-jena") dependsOn core

lazy val pluginsJson = project in file("silk-plugins-json") dependsOn core

lazy val pluginsSpatialTemporal = project in file("silk-plugins-spatial-temporal") dependsOn core

lazy val plugins = project in file(".") dependsOn pluginsJena dependsOn pluginsJson dependsOn pluginsSpatialTemporal