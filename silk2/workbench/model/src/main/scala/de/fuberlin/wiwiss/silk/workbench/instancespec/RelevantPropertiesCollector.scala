package de.fuberlin.wiwiss.silk.workbench.instancespec

import de.fuberlin.wiwiss.silk.util.sparql.SparqlEndpoint
import java.util.logging.Logger

/**
 * Retrieves a list of properties which are defined on most instances.
 */
private object RelevantPropertiesCollector
{
    /** The minimum frequency of a property to be considered relevant */
    private val MinFrequency = 0.7

    /** The maximum number of relevant properties */
    private val MaxPropertyCount = 50

    private val logger = Logger.getLogger(RelevantPropertiesCollector.getClass.getName)

    /**
     * Retrieves a list of properties which are defined on most instances.
     *
     * @param instances The list of instances for which the most frequent properties should be identified
     * @param endpoint The SPARQL endpoint
     * @return The most frequent properties and their frequency
     */
    def apply(instances : Traversable[String], endpoint : SparqlEndpoint) : Traversable[(String, Double)] =
    {
        logger.info("Searching for relevant properties in " + endpoint)

        val instanceArray = instances.toArray

        //Get all properties
        val properties = instanceArray.flatMap(instance => getProperties(instance, endpoint))

        //Compute the frequency of each property
        val propertyFrequencies = properties.groupBy(x => x).mapValues(_.size.toDouble / instanceArray.size).toList

        //Choose the relevant properties
        val relevantProperties = propertyFrequencies.filter{ case (uri, frequency) => frequency > MinFrequency }
                                                    .sortWith(_._2 > _._2).take(MaxPropertyCount)

        logger.info("Found " + relevantProperties.size + " relevant properties in " + endpoint)

        relevantProperties
    }

    private def getProperties(instanceUri : String, endpoint : SparqlEndpoint) : Traversable[String] =
    {
        var sparql = ""
        sparql += "SELECT DISTINCT ?p \n"
        sparql += "WHERE {\n"
        sparql += " <" + instanceUri + "> ?p ?o\n"
        sparql += "}"

        for(result <- endpoint.query(sparql); binding <- result.values) yield binding.value
    }
}
