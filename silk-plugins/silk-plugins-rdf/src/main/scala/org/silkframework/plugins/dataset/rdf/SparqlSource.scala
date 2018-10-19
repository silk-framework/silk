package org.silkframework.plugins.dataset.rdf

import java.util.logging.{Level, Logger}

import org.silkframework.config.{PlainTask, Task}
import org.silkframework.dataset._
import org.silkframework.dataset.rdf.{SparqlEndpoint, SparqlParams}
import org.silkframework.entity.rdf.SparqlRestriction
import org.silkframework.entity.{Entity, EntitySchema, Path, TypedPath}
import org.silkframework.plugins.dataset.rdf.sparql._
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.{Identifier, Uri}

/**
 * A source for reading from SPARQL endpoints.
 */
class SparqlSource(params: SparqlParams, val sparqlEndpoint: SparqlEndpoint) extends DataSource with PeakDataSource with SamplingDataSource {

  private val log = Logger.getLogger(classOf[SparqlSource].getName)

  private val entityUris: Seq[String] = params.entityRestriction

  override def retrieve(entitySchema: EntitySchema, limit: Option[Int] = None)
                       (implicit userContext: UserContext): Traversable[Entity] = {
    val entityRetriever = EntityRetriever(sparqlEndpoint, params.strategy, params.pageSize, params.graph, params.useOrderBy)
    entityRetriever.retrieve(entitySchema, entityUris.map(Uri(_)), limit)
  }

  override def retrieveByUri(entitySchema: EntitySchema, entities: Seq[Uri])
                            (implicit userContext: UserContext): Traversable[Entity] = {
    if(entities.isEmpty) {
      Seq.empty
    } else {
      val entityRetriever = EntityRetriever(sparqlEndpoint, params.strategy, params.pageSize, params.graph, params.useOrderBy)
      entityRetriever.retrieve(entitySchema, entities, None)
    }
  }

  override def retrievePaths(t: Uri, depth: Int = 1, limit: Option[Int] = None)
                            (implicit userContext: UserContext): IndexedSeq[Path] = {
    val restrictions = SparqlRestriction.forType(t)

    //Create an endpoint which fails after 3 retries
    val failFastEndpoint = sparqlEndpoint.withSparqlParams(params.copy(retryCount = 3, retryPause = 1000))

    try {
      SparqlAggregatePathsCollector(failFastEndpoint, params.graph, restrictions, limit)
    } catch {
      case ex: Exception =>
        log.log(Level.INFO, "Failed to retrieve the most frequent paths using a SPARQL 1.1 aggregation query. Falling back to sampling.", ex)
        SparqlSamplePathsCollector(sparqlEndpoint, params.graph, restrictions, limit).toIndexedSeq
    }
  }

  override def retrieveTypes(limit: Option[Int])
                            (implicit userContext: UserContext): Traversable[(String, Double)] = {
    SparqlTypesCollector(sparqlEndpoint, params.graph, limit)
  }

  override def toString: String = sparqlEndpoint.toString

  /**
    * The dataset task underlying the Datset this source belongs to
    *
    * @return
    */
  override def underlyingTask: Task[DatasetSpec[Dataset]] = {
    val taskId = params.graph match{
      case Some(g) => Identifier.fromAllowed(g.substring(g.lastIndexOf("/")))
      case None => Identifier("default_graph")
    }

    PlainTask(taskId, DatasetSpec(EmptyDataset))        //FIXME CMEM 1352 - replace with actual task
  }

  override def sampleValues(typeUri: Option[Uri],
                            typedPaths: Seq[TypedPath],
                            valueSampleLimit: Option[Int])
                           (implicit userContext: UserContext): Seq[Traversable[String]] = {
    typedPaths map { typedPath =>
      new ValueTraverser(typeUri, typedPath, valueSampleLimit)
    }
  }

  class ValueTraverser(typeUri: Option[Uri],
                       typedPath: TypedPath,
                       limit: Option[Int])
                      (implicit userContext: UserContext) extends Traversable[String] {
    override def foreach[U](f: String => U): Unit = {
      val pathQuery = ParallelEntityRetriever.pathQuery(
        "a",
        typeUri.map(SparqlRestriction.forType).getOrElse(SparqlRestriction.empty),
        Path(typedPath.operators),
        useDistinct = false,
        graphUri = params.graph,
        useOrderBy = false,
        varPrefix = "v"
      )
      val results = sparqlEndpoint.select(pathQuery, limit = limit.getOrElse(Int.MaxValue))
      for(result <- results.bindings;
          value <- result.get("v0")) {
        f(value.value)
      }
    }
  }

  /** Fast schema extraction, this implementation ignores the analyzer factory and thus does not allow to analyze values. */
//  override def extractSchema[T](analyzerFactory: ValueAnalyzerFactory[T],
//                                pathLimit: Int,
//                                sampleLimit: Option[Int],
//                                progressFN: Double => Unit): ExtractedSchema[T] = {
//    val entityRetriever = EntityRetriever(sparqlEndpoint, params.strategy, params.pageSize, params.graph, params.useOrderBy)
//    entityRetriever.retrieve(entitySchema, entityUris.map(Uri(_)), limit)
//  }

}