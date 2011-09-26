package de.fuberlin.wiwiss.silk.workbench.util

import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.util.sparql.{Literal, Resource, SparqlEndpoint}

/**
 * Determines which statements are true for all given entities.
 */
private object RestrictionCollector {
    /** The minimum frequency of a value to be considered as a restriction */
    private val FrequencyThreshold = 0.999

    private val logger = Logger.getLogger(RestrictionCollector.getClass.getName)

    /**
     * Determines which statements are true for all given entities.
     *
     * @param entities The set of entities for which the restrictions should be collected
     * @param properties The properties which should be considered for finding the restriction and their frequency
     * @param variable The subject variable which should be used in the generated statements
     * @param endpoint The SPARQL endpoint
     * @return The generated statements
     */
    def apply(entities : Traversable[String], properties : Traversable[(String, Double)], variable : String, endpoint : SparqlEndpoint) : String = {
        logger.info("Finding restrictions in " + endpoint)

        val entityArray = entities.toArray
        val restrictionProperties = properties.filter{case (prop, freq) => freq >= FrequencyThreshold}.map(_._1)

        val restrictions = restrictionProperties.flatMap(property => getRestriction(entityArray, property, variable, endpoint)).mkString(" .\n")

        restrictions
    }

    private def getRestriction(entities : Array[String], property : String, variable : String, endpoint : SparqlEndpoint) : Traversable[String] = {
        //Get all values of this property for all entities
        val values = entities.flatMap(entity => getPropertyValue(entity, property, endpoint))

        //Compute the frequency of each value
        val valueFrequencies = values.groupBy(x => x).mapValues(_.size.toDouble / entities.size)

        //Filter the values which are defined on almost every entity
        val uniformValues = valueFrequencies.filter{case (value, freq) => freq >= FrequencyThreshold}

        //Log result
        for((value, freq) <- uniformValues) {
            logger.info("Found restriction: " + property + " = " + value + " (" + freq + ")")
        }

        //Build the restrictions
        val restrictions = uniformValues.keys.map(value => "?" + variable + " <" + property + "> " + value)

        restrictions
    }

    private def getPropertyValue(entity : String, property : String, endpoint : SparqlEndpoint) : Traversable[String] = {
        var sparql = ""
        sparql += "SELECT DISTINCT ?v \n"
        sparql += "WHERE {\n"
        sparql += " <" + entity + "> <" + property + "> ?v\n"
        sparql += "}"

        for(result <- endpoint.query(sparql); binding <- result.values) yield binding match {
            case Resource(value) => "<" + value + ">"
            case Literal(value) => value
        }
    }
}
