package de.fuberlin.wiwiss.silk.util.sparql

import de.fuberlin.wiwiss.silk.util.Uri
import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.instance.{SparqlRestriction, ForwardOperator, Path}

/**
 * Retrieves the most frequent paths of a number of random sample instances.
 *
 * The limitations of the current implementation are:
 * - It does only return forward paths of length 1
 * - It returns a maximum of 100 paths
 */
object SparqlSamplePathsCollector extends SparqlPathsCollector
{
  /** Number of sample instances */
  private val maxInstances = 100

  /** The minimum frequency of a property to be considered relevant */
  private val MinFrequency = 0.7

  private implicit val logger = Logger.getLogger(SparqlSamplePathsCollector.getClass.getName)

  def apply(endpoint : SparqlEndpoint, restrictions : SparqlRestriction, limit : Option[Int]) : Traversable[(Path, Double)] =
  {
    val variable = restrictions.toSparql.dropWhile(_ != '?').drop(1).takeWhile(_ != ' ')

    val sampleInstances = getInstances(endpoint, restrictions, variable)

    getInstancesPaths(endpoint, sampleInstances, variable, limit.getOrElse(100))
  }

  private def getInstances(endpoint : SparqlEndpoint, restrictions : SparqlRestriction, variable : String) : Traversable[String] =
  {
    val sparql = "SELECT ?" + variable + " WHERE {\n" +
      restrictions.toSparql + ".\n" +
      "}"

    val results = endpoint.query(sparql, maxInstances)

    results.map(_(variable).value)
  }

  private def getInstancesPaths(endpoint : SparqlEndpoint, instances : Traversable[String], variable : String, limit : Int) : Traversable[(Path, Double)] =
  {
    logger.info("Searching for relevant properties in " + endpoint)

    val instanceArray = instances.toArray

    //Get all properties
    val properties = instanceArray.flatMap(instance => getInstanceProperties(endpoint, instance, variable))

    //Compute the frequency of each property
    val propertyFrequencies = properties.groupBy(x => x).mapValues(_.size.toDouble / instanceArray.size).toList

    //Choose the relevant properties
    val relevantProperties = propertyFrequencies.filter{ case (uri, frequency) => frequency > MinFrequency }
      .sortWith(_._2 > _._2).take(limit)

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
      yield new Path(variable, ForwardOperator(Uri.fromURI(binding.value)) :: Nil)
  }
}