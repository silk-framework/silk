package org.silkframework.plugins.dataset.rdf.access

import org.silkframework.config.{PlainTask, Prefixes, Task}
import org.silkframework.dataset._
import org.silkframework.dataset.rdf._
import org.silkframework.entity._
import org.silkframework.entity.paths.{BackwardOperator, ForwardOperator, TypedPath, UntypedPath}
import org.silkframework.entity.rdf.SparqlRestriction
import org.silkframework.execution.EntityHolder
import org.silkframework.execution.local.{EmptyEntityTable, GenericEntityTable}
import org.silkframework.plugins.dataset.rdf.sparql._
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.iterator.CloseableIterator
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.util.{Identifier, Uri}

import java.util.logging.{Level, Logger}
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
                       (implicit context: PluginContext): EntityHolder = {
    val entityRetriever = EntityRetriever(sparqlEndpoint, params.strategy, params.pageSize, params.graph, params.useOrderBy)
    val entities = entityRetriever.retrieve(entitySchema, entityUris.map(Uri(_)), limit)(context.user)
    GenericEntityTable(entities, entitySchema, underlyingTask)
  }

  override def retrieveByUri(entitySchema: EntitySchema, entities: Seq[Uri])
                            (implicit context: PluginContext): EntityHolder = {
    if(entities.isEmpty) {
      EmptyEntityTable(underlyingTask)
    } else {
      val entityRetriever = EntityRetriever(sparqlEndpoint, params.strategy, params.pageSize, params.graph, params.useOrderBy)
      val retrievedEntities = entityRetriever.retrieve(entitySchema, entities, None)(context.user)
      GenericEntityTable(retrievedEntities, entitySchema, underlyingTask)
    }
  }

  override def retrievePaths(typeUri: Uri, depth: Int = 1, limit: Option[Int] = None)
                            (implicit userContext: UserContext, prefixes: Prefixes): IndexedSeq[TypedPath] = {
    val restrictions = if(typeUri.isEmpty) {
      SparqlRestriction.empty
    } else {
      SparqlRestriction.forType(typeUri)
    }
    retrievePathsSparqlRestriction(restrictions, limit)
  }

  override def retrieveTypes(limit: Option[Int])
                            (implicit userContext: UserContext, prefixes: Prefixes): Iterable[(String, Double)] = {
    SparqlTypesCollector(sparqlEndpoint, params.graph, limit)
  }

  override def toString: String = sparqlEndpoint.toString

  /**
    * The dataset task underlying the Datset this source belongs to
    *
    * @return
    */
  override lazy val underlyingTask: Task[DatasetSpec[Dataset]] = {
    val taskId = params.graph match{
      case Some(g) => Identifier.fromAllowed("graph-" + g.reverse.takeWhile(_ != '/').reverse)
      case None => Identifier("default_graph")
    }

    PlainTask(taskId, DatasetSpec(EmptyDataset))        //FIXME CMEM 1352 - replace with actual task
  }

  override def sampleValues(typeUri: Option[Uri],
                            typedPaths: Seq[TypedPath],
                            valueSampleLimit: Option[Int])
                           (implicit userContext: UserContext): Seq[CloseableIterator[String]] = {
    typedPaths map { typedPath =>
      val pathQuery = ParallelEntityRetriever.pathQuery(
        "a",
        typeUri.map(SparqlRestriction.forType).getOrElse(SparqlRestriction.empty),
        UntypedPath(typedPath.operators),
        useDistinct = false,
        graphUri = params.graph,
        useOrderBy = false,
        varPrefix = "v",
        useOptional = false
      )
      val results = sparqlEndpoint.select(pathQuery, limit = valueSampleLimit.getOrElse(Int.MaxValue))
      for (result <- results.bindings;
           value <- result.get("v0")) yield {
        value.value
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
    val classProperties = mutable.HashMap[String, Seq[TypedPath]]()
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

  private def addBackwardPaths(classProperties: mutable.HashMap[String, Seq[TypedPath]])
                              (implicit userContext: UserContext): Unit = {
    val backwardResults = sparqlEndpoint.select(
      """
        |SELECT DISTINCT ?class ?property (SAMPLE(?v) as ?valueSample)
        |WHERE {
        |  ?s a ?class .
        |  ?v ?property ?s
        |} GROUP BY ?class ?property
      """.stripMargin)

    backwardResults.bindings.use { bindings =>
      for (binding <- bindings;
           classResource <- binding.get("class") if classResource.isInstanceOf[Resource]) {
        val classUri = binding("class").value
        val propertyUri = binding("property").value
        val propertyType = valueType(binding("valueSample"))
        val currentProperties = classProperties.getOrElseUpdate(classUri, Nil)
        classProperties.put(classUri, currentProperties :+ TypedPath(BackwardOperator(propertyUri) :: Nil, propertyType, isAttribute = false))
      }
    }
  }

  private def addForwardPaths(classProperties: mutable.HashMap[String, Seq[TypedPath]])
                             (implicit userContext: UserContext): Unit = {
    val forwardResults = sparqlEndpoint.select(
      """
        |SELECT DISTINCT ?class ?property (SAMPLE(?v) as ?valueSample)
        |WHERE {
        |  ?s a ?class ;
        |     ?property ?v
        |} GROUP BY ?class ?property
      """.stripMargin)

    forwardResults.bindings.use { bindings =>
      for (binding <- bindings;
           classResource <- binding.get("class") if classResource.isInstanceOf[Resource]) {
        val classUri = binding("class").value
        val propertyUri = binding("property").value
        val propertyType = valueType(binding("valueSample"))
        val currentProperties = classProperties.getOrElseUpdate(classUri, Nil)
        classProperties.put(classUri, currentProperties :+ TypedPath(ForwardOperator(propertyUri) :: Nil, propertyType, isAttribute = false))
      }
    }
  }

  private def valueType(value: RdfNode): ValueType = {
    value match {
      case _: Resource => ValueType.URI
      case _: BlankNode => ValueType.BLANK_NODE
      case _ => ValueType.STRING
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
      case ex: InterruptedException =>
        throw ex
      case ex: Exception =>
        log.log(Level.INFO, "Failed to retrieve the most frequent paths using a SPARQL 1.1 aggregation query. Falling back to sampling.", ex)
        SparqlSamplePathsCollector(sparqlEndpoint, params.graph, restriction, limit).toIndexedSeq
    }
  }
}

