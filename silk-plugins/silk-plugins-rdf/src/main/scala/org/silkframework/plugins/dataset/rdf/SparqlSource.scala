package org.silkframework.plugins.dataset.rdf

import java.util.logging.{Level, Logger}

import org.silkframework.dataset.DataSource
import org.silkframework.dataset.rdf.SparqlEndpoint
import org.silkframework.entity.rdf.SparqlRestriction
import org.silkframework.entity.{Entity, EntitySchema, Path}
import org.silkframework.plugins.dataset.rdf.endpoint.RemoteSparqlEndpoint
import org.silkframework.plugins.dataset.rdf.sparql._
import org.silkframework.util.Uri

/**
 * A source for reading from SPARQL endpoints.
 */
class SparqlSource(params: SparqlParams, val sparqlEndpoint: SparqlEndpoint) extends DataSource {

  private val log = Logger.getLogger(classOf[SparqlSource].getName)

  private val entityUris = Option(params.entityList).getOrElse("").split(' ').map(_.trim).filter(!_.isEmpty)

  override def retrieve(entitySchema: EntitySchema, limit: Option[Int] = None): Traversable[Entity] = {
    val entityRetriever =
      if(params.parallel)
        new ParallelEntityRetriever(sparqlEndpoint, params.pageSize, params.graph, params.useOrderBy)
      else
        new SimpleEntityRetriever(sparqlEndpoint, params.pageSize, params.graph, params.useOrderBy)

    entityRetriever.retrieve(entitySchema, entityUris.map(Uri(_)), None)
  }

  override def retrieveByUri(entitySchema: EntitySchema, entities: Seq[Uri]): Seq[Entity] = {
    val entityRetriever =
      if(params.parallel)
        new ParallelEntityRetriever(sparqlEndpoint, params.pageSize, params.graph, params.useOrderBy)
      else
        new SimpleEntityRetriever(sparqlEndpoint, params.pageSize, params.graph, params.useOrderBy)

    entityRetriever.retrieve(entitySchema, entities, None).toSeq
  }

  override def retrievePaths(t: Uri, depth: Int = 1, limit: Option[Int] = None): IndexedSeq[Path] = {
    val restrictions = SparqlRestriction.fromSparql("a", s"?a a <$t>.")

    //Create an endpoint which fails after 3 retries
    val failFastEndpoint = new RemoteSparqlEndpoint(params.copy(retryCount = 3, retryPause = 1000))

    try {
      SparqlAggregatePathsCollector(failFastEndpoint, restrictions, limit)
    } catch {
      case ex: Exception =>
        log.log(Level.INFO, "Failed to retrieve the most frequent paths using a SPARQL 1.1 aggregation query. Falling back to sampling.", ex)
        SparqlSamplePathsCollector(sparqlEndpoint, restrictions, limit).toIndexedSeq
    }
  }

  override def retrieveTypes(limit: Option[Int]): Traversable[(String, Double)] = {
    SparqlTypesCollector(sparqlEndpoint, limit)
  }

  override def toString = sparqlEndpoint.toString
}