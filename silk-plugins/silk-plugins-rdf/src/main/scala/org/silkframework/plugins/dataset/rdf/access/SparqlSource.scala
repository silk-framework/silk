package org.silkframework.plugins.dataset.rdf.access

import java.util.logging.{Level, Logger}

import org.silkframework.config.{PlainTask, Task}
import org.silkframework.dataset._
import org.silkframework.dataset.rdf.{Resource, SparqlEndpoint, SparqlParams}
import org.silkframework.entity._
import org.silkframework.entity.paths.{BackwardOperator, TypedPath, UntypedPath}
import org.silkframework.entity.rdf.SparqlRestriction
import org.silkframework.plugins.dataset.rdf.sparql._
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.{Identifier, Uri}

import scala.collection.mutable

/**
 * A source for reading from SPARQL endpoints.
 */
class SparqlSource(params: SparqlParams, val sparqlEndpoint: SparqlEndpoint)
    extends DataSource
    with PeakDataSource
    with SchemaExtractionSource
    with SamplingDataSource
    with SparqlRestrictionDataSource {

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

  override def retrievePaths(typeUri: Uri, depth: Int = 1, limit: Option[Int] = None)
                            (implicit userContext: UserContext): IndexedSeq[TypedPath] = {
    val restrictions = SparqlRestriction.forType(typeUri)
    retrievePathsSparqlRestriction(restrictions, limit)
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
        UntypedPath(typedPath.operators),
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

  /** Fast schema extraction, this implementation ignores the analyzer factory and thus does not allow to analyze values,
    * because else it cannot be done quickly. */
  override def extractSchema[T](analyzerFactory: ValueAnalyzerFactory[T],
                                pathLimit: Int,
                                sampleLimit: Option[Int],
                                progressFN: Double => Unit)
                               (implicit userContext: UserContext): ExtractedSchema[T] = {
    val classProperties = mutable.HashMap[String, List[UntypedPath]]()
    addForwardPaths(classProperties)
    addBackwardPaths(classProperties)
    val schemaClasses = for((classUri, classProperties) <- classProperties) yield {
      val extractedSchemaPaths = classProperties map { property =>
        ExtractedSchemaProperty(property, None.asInstanceOf[Option[T]])
      }
      ExtractedSchemaClass[T](classUri, extractedSchemaPaths)
    }
    ExtractedSchema(schemaClasses.toSeq)
  }

  private def addBackwardPaths[T](classProperties: mutable.HashMap[String, List[UntypedPath]])
                                 (implicit userContext: UserContext): Unit = {
    val backwardResults = sparqlEndpoint.select(
      """
        |SELECT DISTINCT ?class ?property
        |WHERE {
        |  ?s a ?class .
        |  ?v ?property ?s
        |}
      """.stripMargin)
    for (binding <- backwardResults.bindings;
         classResource <- binding.get("class") if classResource.isInstanceOf[Resource]) {
      val classUri = binding("class").value
      val propertyUri = binding("property").value
      val currentProperties = classProperties.getOrElseUpdate(classUri, Nil)
      classProperties.put(classUri, UntypedPath(BackwardOperator(propertyUri) :: Nil) :: currentProperties)
    }
  }

  private def addForwardPaths[T](classProperties: mutable.HashMap[String, List[UntypedPath]])
                                (implicit userContext: UserContext): Unit = {
    val forwardResults = sparqlEndpoint.select(
      """
        |SELECT DISTINCT ?class ?property
        |WHERE {
        |  ?s a ?class ;
        |     ?property ?v
        |}
      """.stripMargin)
    for (binding <- forwardResults.bindings;
         classResource <- binding.get("class") if classResource.isInstanceOf[Resource]) {
      val classUri = binding("class").value
      val propertyUri = binding("property").value
      val currentProperties = classProperties.getOrElseUpdate(classUri, Nil)
      classProperties.put(classUri, UntypedPath(propertyUri) :: currentProperties)
    }
  }

  override def retrievePathsSparqlRestriction(restriction: SparqlRestriction,
                                              limit: Option[Int])
                                             (implicit userContext: UserContext): IndexedSeq[TypedPath] = {
    //Create an endpoint which fails after 3 retries
    val failFastEndpoint = sparqlEndpoint.withSparqlParams(params.copy(retryCount = 3, retryPause = 1000))

    try {
      SparqlAggregatePathsCollector(failFastEndpoint, params.graph, restriction, limit)
    } catch {
      case ex: Exception =>
        log.log(Level.INFO, "Failed to retrieve the most frequent paths using a SPARQL 1.1 aggregation query. Falling back to sampling.", ex)
        SparqlSamplePathsCollector(sparqlEndpoint, params.graph, restriction, limit).toIndexedSeq
    }
  }
}