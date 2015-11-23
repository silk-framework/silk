package org.silkframework.plugins.dataset.rdf

import java.util.logging.{Level, Logger}

import org.silkframework.dataset.DataSource
import org.silkframework.dataset.rdf.SparqlEndpoint
import org.silkframework.entity.rdf.{SparqlRestriction, SparqlEntitySchema}
import org.silkframework.entity.Path
import org.silkframework.plugins.dataset.rdf.endpoint.RemoteSparqlEndpoint
import org.silkframework.plugins.dataset.rdf.sparql._
import org.silkframework.util.Uri

/**
 * A source for reading from SPARQL endpoints.
 */
class SparqlSource(params: SparqlParams, val sparqlEndpoint: SparqlEndpoint) extends DataSource {

  private val log = Logger.getLogger(classOf[SparqlSource].getName)

  private val entityUris = Option(params.entityList).getOrElse("").split(' ').map(_.trim).filter(!_.isEmpty)

  override def retrieveSparqlEntities(entityDesc: SparqlEntitySchema, entities: Seq[String]) = {
    val entityRetriever =
      if(params.parallel)
        new ParallelEntityRetriever(sparqlEndpoint, params.pageSize, params.graph, params.useOrderBy)
      else
        new SimpleEntityRetriever(sparqlEndpoint, params.pageSize, params.graph, params.useOrderBy)

    entityRetriever.retrieve(entityDesc, (entityUris union entities).map(Uri(_)), None)
  }

  override def retrieveSparqlPaths(restrictions: SparqlRestriction, depth: Int, limit: Option[Int]): Traversable[(Path, Double)] = {
    //Create an endpoint which fails after 3 retries
    val failFastEndpoint = new RemoteSparqlEndpoint(params.copy(retryCount = 3, retryPause = 1000))

    try {
      SparqlAggregatePathsCollector(failFastEndpoint, restrictions, limit).map(p => (p, 1.0))
    } catch {
      case ex: Exception =>
        log.log(Level.INFO, "Failed to retrieve the most frequent paths using a SPARQL 1.1 aggregation query. Falling back to sampling.", ex)
        SparqlSamplePathsCollector(sparqlEndpoint, restrictions, limit).map(p => (p, 1.0))
    }
  }

  override def retrieveTypes(limit: Option[Int]): Traversable[(String, Double)] = {
    SparqlTypesCollector(sparqlEndpoint, limit)
  }

  override def toString = sparqlEndpoint.toString
}