package de.fuberlin.wiwiss.silk.config

import de.fuberlin.wiwiss.silk.linkspec.LinkSpecification
import de.fuberlin.wiwiss.silk.output.Output
import de.fuberlin.wiwiss.silk.datasource.Source

/**
 * A Silk linking configuration.
 * Specifies how multiple sources are interlinked by defining a link specification for each type of instance to be interlinked.
 *
 * @param prefixes The prefixes which are used throughout the configuration to shorten URIs
 * @param sources The sources which should be interlinked
 * @param linkSpecs The Silk link specifications
 * @param outputs The global output
 */
case class Configuration(val prefixes : Map[String, String], val sources : Traversable[Source],
                         val linkSpecs : Traversable[LinkSpecification], val outputs : Traversable[Output] = Traversable.empty)
{
    private val sourceMap = sources.map(s => (s.id, s)).toMap
    private val linkSpecMap = linkSpecs.map(s => (s.id, s)).toMap

    def source(id : String) = sourceMap(id)

    def linkSpec(id : String) = linkSpecMap(id)
}
