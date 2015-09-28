package de.fuberlin.wiwiss.silk.plugins.dataset.rdf

import java.util.logging.{Level, Logger}

import de.fuberlin.wiwiss.silk.dataset.DataSource
import de.fuberlin.wiwiss.silk.entity.{EntityDescription, Path, SparqlRestriction}
import de.fuberlin.wiwiss.silk.plugins.dataset.rdf.endpoint.{DefaultHttpEndpoint, HttpEndpoint, RemoteSparqlEndpoint}
import de.fuberlin.wiwiss.silk.plugins.dataset.rdf.sparql._

/**
 * A source for reading from SPARQL endpoints.
 */
class SparqlSource(params: SparqlParams, client: HttpEndpoint = new DefaultHttpEndpoint) extends DataSource {

  private val log = Logger.getLogger(classOf[SparqlSource].getName)

  private val graphUri = Option(params.graph)

  private val entityUris = Option(params.entityList).getOrElse("").split(' ').map(_.trim).filter(!_.isEmpty)

  def sparqlEndpoint = {
    //new JenaRemoteEndpoint(endpointURI)
    new RemoteSparqlEndpoint(params)
  }

  override def retrieve(entityDesc: EntityDescription, entities: Seq[String]) = {
    val entityRetriever =
      if(params.parallel)
        new ParallelEntityRetriever(sparqlEndpoint, params.pageSize, graphUri)
      else
        new SimpleEntityRetriever(sparqlEndpoint, params.pageSize, graphUri)

    entityRetriever.retrieve(entityDesc, entityUris union entities)
  }

  override def retrievePaths(restrictions: SparqlRestriction, depth: Int, limit: Option[Int]): Traversable[(Path, Double)] = {
    //Create an endpoint which fails after 3 retries
    val failFastEndpoint = new RemoteSparqlEndpoint(params.copy(retryCount = 3, retryPause = 1000))

    try {
      SparqlAggregatePathsCollector(failFastEndpoint, restrictions, limit)
    } catch {
      case ex: Exception =>
        log.log(Level.INFO, "Failed to retrieve the most frequent paths using a SPARQL 1.1 aggregation query. Falling back to sampling.", ex)
        SparqlSamplePathsCollector(sparqlEndpoint, restrictions, limit)
    }
  }

  override def retrieveTypes(limit: Option[Int]): Traversable[(String, Double)] = {
    SparqlTypesCollector(sparqlEndpoint, limit)
  }

  override def toString = params.uri
}