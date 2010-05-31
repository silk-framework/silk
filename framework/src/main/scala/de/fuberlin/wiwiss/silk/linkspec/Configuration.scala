package de.fuberlin.wiwiss.silk.linkspec

import de.fuberlin.wiwiss.silk.datasource.DataSource

class Configuration(val prefixes : Map[String, String], val dataSources : Map[String, DataSource],
                    val linkSpecs : Map[String, LinkSpecification])