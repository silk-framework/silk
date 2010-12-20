package de.fuberlin.wiwiss.silk.workbench.instancespec

import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.util.sparql.{Literal, Resource, SparqlEndpoint}

/**
 * Determines which statements are true for all given instances.
 */
private object RestrictionCollector
{
    /** The minimum frequency of a value to be considered as a restriction */
    private val FrequencyThreshold = 0.999

    private val logger = Logger.getLogger(RestrictionCollector.getClass.getName)

    /**
     * Determines which statements are true for all given instances.
     *
     * @param instances The set of instances for which the restrictions should be collected
     * @param properties The properties which should be considered for finding the restriction and their frequency
     * @param variable The subject variable which should be used in the generated statements
     * @param endpoint The SPARQL endpoint
     * @return The generated statements
     */
    def apply(instances : Traversable[String], properties : Traversable[(String, Double)], variable : String, endpoint : SparqlEndpoint) : String =
    {
        logger.info("Finding restrictions in " + endpoint)

        val instanceArray = instances.toArray
        val restrictionProperties = properties.filter{case (prop, freq) => freq >= FrequencyThreshold}.map(_._1)

        val restrictions = restrictionProperties.flatMap(property => getRestriction(instanceArray, property, variable, endpoint)).mkString(" .\n")

        restrictions
    }

    private def getRestriction(instances : Array[String], property : String, variable : String, endpoint : SparqlEndpoint) : Traversable[String] =
    {
        //Get all values of this property for all instances
        val values = instances.flatMap(instance => getPropertyValue(instance, property, endpoint))

        //Compute the frequency of each value
        val valueFrequencies = values.groupBy(x => x).mapValues(_.size.toDouble / instances.size)

        //Filter the values which are defined on almost every instance
        val uniformValues = valueFrequencies.filter{case (value, freq) => freq >= FrequencyThreshold}

        //Log result
        for((value, freq) <- uniformValues)
        {
            logger.info("Found restriction: " + property + " = " + value + " (" + freq + ")")
        }

        //Build the restrictions
        val restrictions = uniformValues.keys.map(value => "?" + variable + " <" + property + "> " + value)

        restrictions
    }

    private def getPropertyValue(instance : String, property : String, endpoint : SparqlEndpoint) : Traversable[String] =
    {
        var sparql = ""
        sparql += "SELECT DISTINCT ?v \n"
        sparql += "WHERE {\n"
        sparql += " <" + instance + "> <" + property + "> ?v\n"
        sparql += "}"

        for(result <- endpoint.query(sparql); binding <- result.values) yield binding match
        {
            case Resource(value) => "<" + value + ">"
            case Literal(value) => value
        }
    }
}
