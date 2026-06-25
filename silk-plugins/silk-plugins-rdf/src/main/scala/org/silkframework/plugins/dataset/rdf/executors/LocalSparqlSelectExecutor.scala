package org.silkframework.plugins.dataset.rdf.executors

import org.silkframework.config.{Prefixes, Task, TaskSpec}
import org.silkframework.dataset.{DataSource, DatasetSpec}
import org.silkframework.dataset.rdf.{RdfDataset, RdfDatasetAccess, RdfNode, SparqlEndpoint, SparqlResults}
import org.silkframework.entity.paths.{TypedPath, UntypedPath}
import org.silkframework.entity.{Entity, EntitySchema, ValueType}
import org.silkframework.execution.local.{GenericEntityTable, LocalEntities, LocalExecution, LocalExecutor}
import org.silkframework.execution.typed.SparqlEndpointEntitySchema
import org.silkframework.execution.{ExecutionReport, ExecutionReportUpdater, ExecutorOutput, ReportingIterator, TaskException}
import org.silkframework.plugins.dataset.rdf.DefaultRdfDataset
import org.silkframework.plugins.dataset.rdf.tasks.SparqlSelectCustomTask
import org.silkframework.plugins.dataset.rdf.tasks.templating.TaskProperties
import org.silkframework.runtime.activity.{ActivityContext, UserContext}
import org.silkframework.runtime.iterator.{AbstractRewindableEntityIterator, CloseableIterator}
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.util.Uri

import java.io.Closeable
import scala.collection.immutable.SortedMap

/**
  * Local executor for [[SparqlSelectCustomTask]].
  */
case class LocalSparqlSelectExecutor() extends LocalExecutor[SparqlSelectCustomTask] {
  override def execute(task: Task[SparqlSelectCustomTask],
                       inputs: Seq[LocalEntities],
                       output: ExecutorOutput,
                       execution: LocalExecution,
                       context: ActivityContext[ExecutionReport])
                      (implicit pluginContext: PluginContext): Option[LocalEntities] = {
    val taskData = task.data
    implicit val prefixes: Prefixes = pluginContext.prefixes
    implicit val executionReportUpdater: SparqlSelectExecutionReportUpdater = SparqlSelectExecutionReportUpdater(task, context)

    inputs match {
      case Seq(SparqlEndpointEntitySchema(sparql)) =>
        // Read the SPARQL endpoint of this execution (not the shared dataset endpoint, which a concurrent
        // execution sharing the same workflow-scoped dataset task may have overwritten).
        val endpoint = RdfDatasetAccess.forExecution(sparql.task, execution).sparqlEndpoint
        val entities = new LocalSparqlSelectIterator(taskData, endpoint, Some(sparql.task), output.task, executionReportUpdater = Some(executionReportUpdater))
        Some(ReportingIterator.addReporter(GenericEntityTable(entities, entitySchema = entities.effectiveSchema, task)))
      case Seq() if taskData.useDefaultDataset =>
        val rdfDataset = DefaultRdfDataset.resolve()
        val entities = executeOnDefaultDataset(taskData, rdfDataset, output.task, executionReportUpdater = Some(executionReportUpdater))
        Some(ReportingIterator.addReporter(GenericEntityTable(entities, entitySchema = entities.effectiveSchema, task)))
      case Seq(input) if taskData.useDefaultDataset =>
        val rdfDataset = DefaultRdfDataset.resolve()
        val (entities, schema) = executeOnDefaultDatasetPerEntity(taskData, rdfDataset, input, output.task, executionReportUpdater)
        Some(ReportingIterator.addReporter(GenericEntityTable(entities, entitySchema = schema, task)))
      case _ =>
        throw TaskException("SPARQL select executor did not receive a SPARQL endpoint as requested!")
    }
  }

  def executeOnSparqlEndpoint(sparqlSelectTask: SparqlSelectCustomTask,
                              inputTask: Task[DatasetSpec[RdfDataset]],
                              outputTask: Option[Task[_ <: TaskSpec]],
                              limit: Int = Integer.MAX_VALUE,
                              executionReportUpdater: Option[SparqlSelectExecutionReportUpdater])
                             (implicit pluginContext: PluginContext): LocalSparqlSelectIterator = {
    new LocalSparqlSelectIterator(sparqlSelectTask, inputTask.data.plugin.sparqlEndpoint, Some(inputTask), outputTask, limit, executionReportUpdater)
  }

  private def executeOnDefaultDataset(sparqlSelectTask: SparqlSelectCustomTask,
                                      rdfDataset: RdfDataset,
                                      outputTask: Option[Task[_ <: TaskSpec]],
                                      limit: Int = Integer.MAX_VALUE,
                                      executionReportUpdater: Option[SparqlSelectExecutionReportUpdater])
                                     (implicit pluginContext: PluginContext): LocalSparqlSelectIterator = {
    new LocalSparqlSelectIterator(sparqlSelectTask, rdfDataset.sparqlEndpoint, None, outputTask, limit, executionReportUpdater)
  }

  /**
   * Executes one query per input entity and returns the resulting rows together with the effective output schema.
   *
   * When the task's output schema can be inferred from the template it is used as-is (the input port advertises a
   * fixed schema). Otherwise the schema is derived at runtime from the actual SELECT result variables: the first
   * input entity's query is executed eagerly to read its result header (which is available before the bindings are
   * consumed), and the remaining entities are processed lazily.
   */
  def executeOnDefaultDatasetPerEntity(sparqlSelectTask: SparqlSelectCustomTask,
                                       rdfDataset: RdfDataset,
                                       input: LocalEntities,
                                       outputTask: Option[Task[_ <: TaskSpec]],
                                       executionReportUpdater: SparqlSelectExecutionReportUpdater,
                                       limit: Int = Integer.MAX_VALUE)
                                      (implicit pluginContext: PluginContext): (CloseableIterator[Entity], EntitySchema) = {
    implicit val user: UserContext = pluginContext.user
    val sparqlEndpoint = rdfDataset.sparqlEndpoint
    val selectLimit = math.min(sparqlSelectTask.intLimit.getOrElse(Integer.MAX_VALUE), limit)
    val taskProperties = TaskProperties.create(Some(input.task), outputTask, pluginContext)
    val templateVariables = pluginContext.templateVariables.all.variables
    val expectedSchema = sparqlSelectTask.expectedInputSchema

    def runEntity(entity: Entity): SparqlResults = {
      val values = expectedSchema.typedPaths.map(tp => entity.valueOfPath(tp.toUntypedPath))
      val projected = Entity(entity.uri, values, expectedSchema)
      val query = sparqlSelectTask.queryTemplate.generate(Some(projected), taskProperties, templateVariables).head
      executionReportUpdater.increaseQueryCounter()
      LocalSparqlSelectIterator.executeSelect(sparqlEndpoint, query, selectLimit, Some(sparqlSelectTask.sparqlTimeout))
    }

    if (sparqlSelectTask.outputSchema.typedPaths.nonEmpty) {
      // Static schema known: preserve the existing fully-lazy behavior (including multi-query-per-entity templates).
      val schema = sparqlSelectTask.outputSchema
      val vars = LocalSparqlSelectIterator.getSparqlVars(schema)
      val bindings = input.entities.flatMap { entity =>
        val values = expectedSchema.typedPaths.map(tp => entity.valueOfPath(tp.toUntypedPath))
        val projected = Entity(entity.uri, values, expectedSchema)
        val queries = sparqlSelectTask.queryTemplate.generate(Some(projected), taskProperties, templateVariables)
        queries.iterator.flatMap { query =>
          executionReportUpdater.increaseQueryCounter()
          LocalSparqlSelectIterator.executeSelect(sparqlEndpoint, query, selectLimit, Some(sparqlSelectTask.sparqlTimeout)).bindings
        }
      }
      (LocalSparqlSelectIterator.createEntities(schema, bindings, vars), schema)
    } else {
      // Fallback: derive the schema from the first executed query's result variables.
      val inputIterator = input.entities
      if (!inputIterator.hasNext) {
        // No input entities -> no queries -> no runtime variables available.
        (CloseableIterator.empty, LocalSparqlSelectIterator.schemaFromVariables(Seq.empty))
      } else {
        val firstResults = runEntity(inputIterator.next())
        val schema = LocalSparqlSelectIterator.schemaFromVariables(firstResults.variables)
        val vars = LocalSparqlSelectIterator.getSparqlVars(schema)
        val restBindings = inputIterator.flatMap(entity => runEntity(entity).bindings)
        val allBindings = CloseableIterator(firstResults.bindings ++ restBindings, new Closeable {
          override def close(): Unit = {
            try firstResults.close() finally restBindings.close()
          }
        })
        (LocalSparqlSelectIterator.createEntities(schema, allBindings, vars), schema)
      }
    }
  }
}

/**
 * Rewindable iterator that executes a SPARQL SELECT query and returns the results as entities.
 */
class LocalSparqlSelectIterator(sparqlSelectTask: SparqlSelectCustomTask,
                                sparqlEndpoint: SparqlEndpoint,
                                inputTask: Option[Task[_ <: TaskSpec]],
                                outputTask: Option[Task[_ <: TaskSpec]],
                                limit: Int = Integer.MAX_VALUE,
                                executionReportUpdater: Option[SparqlSelectExecutionReportUpdater])
                               (implicit pluginContext: PluginContext) extends AbstractRewindableEntityIterator {
  private implicit val user: UserContext = pluginContext.user

  // Effective schema captured during the (cached) first execution, before the bindings are consumed.
  @volatile private var capturedSchema: Option[EntitySchema] = None

  override def newIterator(): CloseableIterator[Entity] = {
    val selectLimit = math.min(sparqlSelectTask.intLimit.getOrElse(Integer.MAX_VALUE), limit)
    val taskProperties = TaskProperties.create(inputTask, outputTask, pluginContext)
    val templateVariables = pluginContext.templateVariables.all.variables
    val query = sparqlSelectTask.queryTemplate.generate(None, taskProperties, templateVariables).head
    executionReportUpdater.foreach(_.increaseQueryCounter())
    val results = LocalSparqlSelectIterator.executeSelect(sparqlEndpoint, query, selectLimit, Some(sparqlSelectTask.sparqlTimeout))
    // results.variables is available here, before the lazy bindings iterator is consumed, so deriving the schema
    // from it does not break streaming.
    val schema = LocalSparqlSelectIterator.effectiveSchema(sparqlSelectTask, results.variables)
    capturedSchema = Some(schema)
    val vars = LocalSparqlSelectIterator.getSparqlVars(schema)
    LocalSparqlSelectIterator.createEntities(schema, results.bindings, vars)
  }

  /**
   * The effective entity schema of the produced rows. Equals the task's statically inferred output schema when that
   * is known; otherwise it is derived from the actual SELECT result variables. In the latter case the first query
   * execution is forced (and cached, so the same iterator is reused without re-executing) to read the result header.
   */
  def effectiveSchema: EntitySchema = {
    if (sparqlSelectTask.outputSchema.typedPaths.nonEmpty) {
      // Fast path: schema is known statically, no need to contact the endpoint.
      sparqlSelectTask.outputSchema
    } else {
      hasNext // Forces (and caches) the first execution, which sets capturedSchema.
      capturedSchema.getOrElse(LocalSparqlSelectIterator.schemaFromVariables(Seq.empty))
    }
  }
}

object LocalSparqlSelectIterator {
  /**
   * Executes the select query on the SPARQL endpoint.
   *
   * @param query   The SELECT query to execute
   * @param limit   The max. number of rows to fetch
   * @param timeout An optional timeout in ms for the query execution. If defined it should have an positive value, else it will be ignored.
   *                This timeout is passed to the underlying SPARQL endpoint implementation.
   */
  def executeSelect(sparqlEndpoint: SparqlEndpoint,
                    query: String,
                    limit: Int = Integer.MAX_VALUE,
                    timeout: Option[Int] = None)
                   (implicit userContext: UserContext): SparqlResults = {
    timeout match {
      case Some(timeoutInMs) if timeoutInMs > 0 =>
        val updatedParams = sparqlEndpoint.sparqlParams.copy(timeout = timeout)
        sparqlEndpoint.withSparqlParams(updatedParams).select(query, limit)
      case _ =>
        sparqlEndpoint.select(query, limit)
    }
  }

  /** Builds an entity schema from the actual SELECT projection variables (mirrors SparqlJinjaTemplate.outputSchema). */
  def schemaFromVariables(variables: Seq[String]): EntitySchema = {
    EntitySchema(
      typeUri = Uri(""),
      typedPaths = variables.map(v => TypedPath(UntypedPath(v), ValueType.STRING, isAttribute = false)).toIndexedSeq
    )
  }

  /**
   * The schema to use for the produced entities: the task's statically inferred output schema when it is known,
   * otherwise a schema derived from the actual runtime SELECT result variables.
   */
  def effectiveSchema(taskData: SparqlSelectCustomTask, runtimeVariables: Seq[String]): EntitySchema = {
    if (taskData.outputSchema.typedPaths.nonEmpty) {
      taskData.outputSchema
    } else {
      schemaFromVariables(runtimeVariables)
    }
  }

  def getSparqlVars(schema: EntitySchema): IndexedSeq[String] = {
    schema.typedPaths map { v =>
      v.propertyUri match {
        case Some(prop) =>
          prop.uri
        case _ =>
          throw TaskException("Path in input schema of SPARQL select operator is not a simple forward property: " + v.toUntypedPath.normalizedSerialization)
      }
    }
  }

  def createEntities(schema: EntitySchema,
                     bindings: CloseableIterator[SortedMap[String, RdfNode]],
                     vars: IndexedSeq[String]): CloseableIterator[Entity] = {
    var count = 0
    bindings.map { binding =>
      count += 1
      val values = vars.map(v => binding.get(v).toSeq.map(_.value))
      Entity(DataSource.URN_NID_PREFIX + count, values = values, schema = schema)
    }
  }
}

case class SparqlSelectExecutionReportUpdater(task: Task[TaskSpec],
                                              context: ActivityContext[ExecutionReport]) extends ExecutionReportUpdater {

  private var queriesStarted = 0

  def increaseQueryCounter(): Unit = {
    queriesStarted += 1
  }

  override def entityLabelSingle: String = "Row"

  override def entityLabelPlural: String = "Rows"

  override def entityProcessVerb: String = {
    val queryWord = if (queriesStarted == 1) "query" else "queries"
    s"processed ($queriesStarted $queryWord)"
  }

  override def additionalFields(): Seq[(String, String)] = {
    Seq("Queries" -> queriesStarted.toString).filter(_ => queriesStarted > 0)
  }
}