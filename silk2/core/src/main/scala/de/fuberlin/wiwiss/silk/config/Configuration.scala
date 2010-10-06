package de.fuberlin.wiwiss.silk.config

import de.fuberlin.wiwiss.silk.linkspec.LinkSpecification
import de.fuberlin.wiwiss.silk.output.Output
import de.fuberlin.wiwiss.silk.datasource.Source

case class Configuration(val prefixes : Map[String, String], val sources : Traversable[Source],
                         val linkSpecs : Traversable[LinkSpecification], val outputs : Traversable[Output] = Traversable.empty)
{
    private val sourceMap = sources.map(s => (s.id, s)).toMap
    private val linkSpecMap = linkSpecs.map(s => (s.id, s)).toMap

    def dataSource(id : String) = sourceMap.get(id)

    def linkSpec(id : String) = linkSpecMap.get(id)
}
