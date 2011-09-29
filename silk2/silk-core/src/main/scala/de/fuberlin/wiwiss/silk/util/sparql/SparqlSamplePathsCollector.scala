package de.fuberlin.wiwiss.silk.util.sparql

import de.fuberlin.wiwiss.silk.util.Uri
import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.entity.{SparqlRestriction, ForwardOperator, Path}

/**
 * Retrieves the most frequent paths of a number of random sample entities.
 *
 * It is typically faster than SparqlAggregatePathsCollector but also less precise.
 *
 * The limitations of the current implementation are:
 * - It does only return forward paths of length 1
 * - It returns a maximum of 100 paths
 */
object SparqlSamplePathsCollector extends SparqlPathsCollector {
  /**Number of sample entities */
  private val maxEntities = 100

  /**The minimum frequency of a property to be considered relevant */
  private val MinFrequency = 0.7

  private implicit val logger = Logger.getLogger(SparqlSamplePathsCollector.getClass.getName)

  def apply(endpoint: SparqlEndpoint, restrictions: SparqlRestriction, limit: Option[Int]): Traversable[(Path, Double)] = {
    val variable = restrictions.toSparql.dropWhile(_ != '?').drop(1).takeWhile(_ != ' ')

    val sampleEntities = {
      if (variable.isEmpty)
        getAllEntities(endpoint)
      else
        getEntities(endpoint, restrictions, variable)
    }

    getEntitiesPaths(endpoint, sampleEntities, variable, limit.getOrElse(100))
  }

  private def getAllEntities(endpoint: SparqlEndpoint): Traversable[String] = {
    val sparql = "SELECT ?s WHERE { ?s ?p ?o } LIMIT " + maxEntities

    val results = endpoint.query(sparql, maxEntities)

    results.map(_("s").value)
  }

  private def getEntities(endpoint: SparqlEndpoint, restrictions: SparqlRestriction, variable: String): Traversable[String] = {
    val sparql = "SELECT ?" + variable + " WHERE { " + restrictions.toSparql + " } LIMIT " + maxEntities

    val results = endpoint.query(sparql, maxEntities)

    results.map(_(variable).value)
  }

  private def getEntitiesPaths(endpoint: SparqlEndpoint, entities: Traversable[String], variable: String, limit: Int): Traversable[(Path, Double)] = {
    logger.info("Searching for relevant properties in " + endpoint)

    val entityArray = entities.toArray

    //Get all properties
    val properties = entityArray.flatMap(entity => getEntityProperties(endpoint, entity, variable, limit))

    //Compute the frequency of each property
    val propertyFrequencies = properties.groupBy(x => x).mapValues(_.size.toDouble / entityArray.size).toList

    //Choose the relevant properties
    val relevantProperties = propertyFrequencies.filter { case (uri, frequency) => frequency > MinFrequency }
                                                .sortWith(_._2 > _._2).take(limit)

    logger.info("Found " + relevantProperties.size + " relevant properties in " + endpoint)

    relevantProperties
  }

  private def getEntityProperties(endpoint: SparqlEndpoint, entityUri: String, variable: String, limit: Int): Traversable[Path] = {
    var sparql = ""
    sparql += "SELECT DISTINCT ?p \n"
    sparql += "WHERE {\n"
    sparql += " <" + entityUri + "> ?p ?o\n"
    sparql += "} LIMIT " + limit

    for (result <- endpoint.query(sparql); binding <- result.values) yield
      Path(variable, ForwardOperator(Uri.fromURI(binding.value)) :: Nil)
  }
}