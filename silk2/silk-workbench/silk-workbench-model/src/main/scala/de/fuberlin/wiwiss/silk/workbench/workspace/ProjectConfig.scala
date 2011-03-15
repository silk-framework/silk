package de.fuberlin.wiwiss.silk.workbench.workspace

import de.fuberlin.wiwiss.silk.config.Prefixes

case class ProjectConfig(prefixes : Prefixes = Prefixes.empty)

object ProjectConfig
{
  lazy val default =
  {
    val prefixes = new Prefixes(Map(
      "rdf" -> "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
      "rdfs" -> "http://www.w3.org/2000/01/rdf-schema#",
      "owl" -> "http://www.w3.org/2002/07/owl#" ))

    ProjectConfig(prefixes)
  }
}