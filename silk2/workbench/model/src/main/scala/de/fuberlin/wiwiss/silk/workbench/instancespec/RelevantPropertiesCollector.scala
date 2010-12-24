package de.fuberlin.wiwiss.silk.workbench.instancespec

import de.fuberlin.wiwiss.silk.util.sparql.SparqlEndpoint
import de.fuberlin.wiwiss.silk.util.Uri
import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.instance.{Path, ForwardOperator}

/**
 * Retrieves the most frequent property paths.
 */
object RelevantPropertiesCollector
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
  def apply(endpoint : SparqlEndpoint, restriction : String, instances : Traversable[String]) : Traversable[(Path, Double)] =
  {
    val variable = restriction.dropWhile(_ != '?').drop(1).takeWhile(_ != ' ')

    if(instances.size < 10)
    {
      getAllPaths(endpoint, restriction, variable)
    }
    else
    {
      getInstancesPaths(endpoint, instances, variable)
    }
  }

  private def getAllPaths(endpoint : SparqlEndpoint, restriction : String, variable : String) : Traversable[(Path, Double)] =
  {
    val sparql = "SELECT ?p ( count(?" + variable + ") AS ?count ) WHERE {\n" +
      restriction + ".\n" +
      "?" + variable + " ?p ?o\n" +
      "}\n" +
      "GROUP BY ?p\n" +
      "ORDER BY DESC (?count)"

    val results = endpoint.query(sparql).toList
    if(!results.isEmpty)
    {
      val maxCount = results.head("count").value.toDouble
      for(result <- results) yield
      {
        (new Path(variable, ForwardOperator(Uri.fromURI(result("p").value, endpoint.prefixes)) :: Nil),
         result("count").value.toDouble / maxCount)
      }
    }
    else
    {
      Traversable.empty
    }
  }

  private def getInstancesPaths(endpoint : SparqlEndpoint, instances : Traversable[String], variable : String) : Traversable[(Path, Double)] =
  {
    logger.info("Searching for relevant properties in " + endpoint)

    val instanceArray = instances.toArray

    //Get all properties
    val properties = instanceArray.flatMap(instance => getInstanceProperties(endpoint, instance, variable))

    //Compute the frequency of each property
    val propertyFrequencies = properties.groupBy(x => x).mapValues(_.size.toDouble / instanceArray.size).toList

    //Choose the relevant properties
    val relevantProperties = propertyFrequencies.filter{ case (uri, frequency) => frequency > MinFrequency }
      .sortWith(_._2 > _._2).take(MaxPropertyCount)

    logger.info("Found " + relevantProperties.size + " relevant properties in " + endpoint)

    relevantProperties
  }

  private def getInstanceProperties(endpoint : SparqlEndpoint, instanceUri : String, variable : String) : Traversable[Path] =
  {
    var sparql = ""
    sparql += "SELECT DISTINCT ?p \n"
    sparql += "WHERE {\n"
    sparql += " <" + instanceUri + "> ?p ?o\n"
    sparql += "}"

    for(result <- endpoint.query(sparql); binding <- result.values)
      yield new Path(variable, ForwardOperator(Uri.fromURI(binding.value, endpoint.prefixes)) :: Nil)
  }
}
