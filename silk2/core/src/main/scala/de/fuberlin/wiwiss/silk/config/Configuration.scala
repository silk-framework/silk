package de.fuberlin.wiwiss.silk.config

import de.fuberlin.wiwiss.silk.datasource.DataSource
import de.fuberlin.wiwiss.silk.linkspec.LinkSpecification

class Configuration(val prefixes : Map[String, String], val dataSources : Map[String, DataSource],
                    val linkSpecs : Map[String, LinkSpecification])
{
}