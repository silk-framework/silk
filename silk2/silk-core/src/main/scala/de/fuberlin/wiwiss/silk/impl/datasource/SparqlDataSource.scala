package de.fuberlin.wiwiss.silk.impl.datasource

import de.fuberlin.wiwiss.silk.datasource.DataSource
import java.net.URI
import de.fuberlin.wiwiss.silk.util.strategy.StrategyAnnotation
import de.fuberlin.wiwiss.silk.util.sparql.{SparqlSamplePathsCollector, SparqlAggregatePathsCollector, EntityRetriever, RemoteSparqlEndpoint}
import java.util.logging.{Level, Logger}
import de.fuberlin.wiwiss.silk.entity.{SparqlRestriction, Path, EntityDescription}

/**
 * DataSource which retrieves all entities from a SPARQL endpoint
 *
 * Parameters:
 * - '''endpointURI''': The URI of the SPARQL endpoint
 * - '''login (optional)''': Login required for authentication
 * - '''password (optional)''': Password required for authentication
 * - '''graph (optional)''': Only retrieve entities from a specific graph
 * - '''pageSize (optional)''': The number of solutions to be retrieved per SPARQL query (default: 1000)
 * - '''entityList (optional)''': A list of entities to be retrieved. If not given, all entities will be retrieved. Multiple entities are separated by a space.
 * - '''pauseTime (optional)''': The number of milliseconds to wait between subsequent query 
 * - '''retryCount (optional)''': The number of retires if a query fails
 * - '''retryPause (optional)''': The number of milliseconds to wait until a failed query is retried
 */
@StrategyAnnotation(id = "sparqlEndpoint", label = "SPARQL Endpoint", description = "DataSource which retrieves all entities from a SPARQL endpoint")
class SparqlDataSource(endpointURI: String, login: String = null, password: String = null,
                       graph: String = null, pageSize: Int = 1000, entityList: String = null,
                       pauseTime: Int = 0, retryCount: Int = 3, retryPause: Int = 1000) extends DataSource {
  private val uri = new URI(endpointURI)

  private val loginComplete = {
    if (login != null) {
      require(password != null, "No password provided for login '" + login + "'. Please set the 'password' parameter.")
      Some((login, password))
    } else {
      None
    }
  }

  private val graphUri = if (graph == null) None else Some(graph)

  private val entityUris = Option(entityList).getOrElse("").split(' ').map(_.trim).filter(!_.isEmpty)

  private val logger = Logger.getLogger(SparqlDataSource.getClass.getName)

  override def retrieve(entityDesc: EntityDescription, entities: Seq[String]) = {
    val entityRetriever = EntityRetriever(createEndpoint(), pageSize, graphUri)

    entityRetriever.retrieve(entityDesc, entityUris union entities)
  }

  override def retrievePaths(restrictions: SparqlRestriction, depth: Int, limit: Option[Int]): Traversable[(Path, Double)] = {
    //Create an endpoint which fails after 3 retries
    val failFastEndpoint = new RemoteSparqlEndpoint(uri, loginComplete, pageSize, pauseTime, 3, 1000)

    try {
      SparqlAggregatePathsCollector(failFastEndpoint, restrictions, limit)
    } catch {
      case ex: Exception => {
        logger.log(Level.INFO, "Failed to retrieve the most frequent paths using a SPARQL 1.1 aggregation query. Falling back to sampling.", ex)

        SparqlSamplePathsCollector(createEndpoint(), restrictions, limit)
      }
    }
  }

  protected def createEndpoint() = {
    new RemoteSparqlEndpoint(uri, loginComplete, pageSize, pauseTime, retryCount, retryPause)
  }

  override def toString = endpointURI
}
