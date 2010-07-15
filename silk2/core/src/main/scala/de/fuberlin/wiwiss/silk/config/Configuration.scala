package de.fuberlin.wiwiss.silk.config

import de.fuberlin.wiwiss.silk.datasource.DataSource
import de.fuberlin.wiwiss.silk.linkspec.LinkSpecification
import de.fuberlin.wiwiss.silk.output.Output

case class Configuration(val prefixes : Map[String, String], val dataSources : Map[String, DataSource],
                         val linkSpecs : Map[String, LinkSpecification], val outputs : Traversable[Output] = Traversable.empty)
{
}
