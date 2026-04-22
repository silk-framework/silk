package org.silkframework.plugins.dataset.rdf.executors

import org.silkframework.config.{Prefixes, Task, TaskSpec}
import org.silkframework.dataset.{DataSource, DatasetSpec}
import org.silkframework.dataset.rdf.{RdfDataset, RdfNode, SparqlEndpoint, SparqlResults}
import org.silkframework.entity.Entity
import org.silkframework.execution.local.{GenericEntityTable, LocalEntities, LocalExecution, LocalExecutor}
import org.silkframework.execution.typed.SparqlEndpointEntitySchema
import org.silkframework.execution.{ExecutionReport, ExecutionReportUpdater, ExecutorOutput, TaskException}
import org.silkframework.plugins.dataset.rdf.DefaultRdfDataset
import org.silkframework.plugins.dataset.rdf.tasks.SparqlSelectCustomTask
import org.silkframework.plugins.dataset.rdf.tasks.templating.TaskProperties
import org.silkframework.runtime.activity.{ActivityContext, UserContext}
import org.silkframework.runtime.iterator.CloseableIterator
import org.silkframework.runtime.plugin.PluginContext

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
    implicit val executionReportUpdater: SparqlSelectExecutionReportUpdater = SparqlSelectExecutionReportUpdater(task, context)

    inputs match {
      case Seq(SparqlEndpointEntitySchema(sparql)) =>
        val entities = executeOnSparqlEndpoint(taskData, sparql.task, output.task, executionReportUpdater = Some(executionReportUpdater))
        Some(GenericEntityTable(entities, entitySchema = taskData.outputSchema, task))
      case Seq() if taskData.useDefaultDataset =>
        val rdfDataset = DefaultRdfDataset.resolve()
        val entities = executeOnDefaultDataset(taskData, rdfDataset, output.task, executionReportUpdater = Some(executionReportUpdater))
        Some(GenericEntityTable(entities, entitySchema = taskData.outputSchema, task))
      case Seq(input) if taskData.useDefaultDataset =>
        val rdfDataset = DefaultRdfDataset.resolve()
        val entities = executeOnDefaultDatasetPerEntity(taskData, rdfDataset, input, output.task, executionReportUpdater = Some(executionReportUpdater))
        Some(GenericEntityTable(entities, entitySchema = taskData.outputSchema, task))
      case _ =>
        throw TaskException("SPARQL select executor did not receive a SPARQL endpoint as requested!")
    }
  }

  def executeOnSparqlEndpoint(sparqlSelectTask: SparqlSelectCustomTask,
                              inputTask: Task[DatasetSpec[RdfDataset]],
                              outputTask: Option[Task[_ <: TaskSpec]],
                              limit: Int = Integer.MAX_VALUE,
                              executionReportUpdater: Option[SparqlSelectExecutionReportUpdater])
                             (implicit pluginContext: PluginContext): CloseableIterator[Entity] = {
    runSelect(sparqlSelectTask, inputTask.data.plugin.sparqlEndpoint, Some(inputTask), outputTask, limit, executionReportUpdater)
  }

  private def executeOnDefaultDataset(sparqlSelectTask: SparqlSelectCustomTask,
                                      rdfDataset: RdfDataset,
                                      outputTask: Option[Task[_ <: TaskSpec]],
                                      limit: Int = Integer.MAX_VALUE,
                                      executionReportUpdater: Option[SparqlSelectExecutionReportUpdater])
                                     (implicit pluginContext: PluginContext): CloseableIterator[Entity] = {
    runSelect(sparqlSelectTask, rdfDataset.sparqlEndpoint, None, outputTask, limit, executionReportUpdater)
  }

  def executeOnDefaultDatasetPerEntity(sparqlSelectTask: SparqlSelectCustomTask,
                                       rdfDataset: RdfDataset,
                                       input: LocalEntities,
                                       outputTask: Option[Task[_ <: TaskSpec]],
                                       limit: Int = Integer.MAX_VALUE,
                                       executionReportUpdater: Option[SparqlSelectExecutionReportUpdater])
                                      (implicit pluginContext: PluginContext): CloseableIterator[Entity] = {
    implicit val user: UserContext = pluginContext.user
    val sparqlEndpoint = rdfDataset.sparqlEndpoint
    val selectLimit = math.min(sparqlSelectTask.intLimit.getOrElse(Integer.MAX_VALUE), limit)
    val taskProperties = TaskProperties.create(Some(input.task), outputTask, pluginContext)
    val templateVariables = pluginContext.templateVariables.all.variables
    val expectedSchema = sparqlSelectTask.expectedInputSchema
    val vars = getSparqlVars(sparqlSelectTask)

    val bindings = input.entities.flatMap { entity =>
      val values = expectedSchema.typedPaths.map(tp => entity.valueOfPath(tp.toUntypedPath))
      if (values.forall(_.nonEmpty)) {
        val projected = Entity(entity.uri, values, expectedSchema)
        val queries = sparqlSelectTask.queryTemplate.generate(Some(projected), taskProperties, templateVariables)
        queries.iterator.flatMap { query =>
          executeSelect(sparqlEndpoint, query, selectLimit, Some(sparqlSelectTask.sparqlTimeout)).bindings
        }
      } else {
        Iterator.empty
      }
    }
    createEntities(sparqlSelectTask, bindings, vars, executionReportUpdater)
  }

  private def runSelect(sparqlSelectTask: SparqlSelectCustomTask,
                        sparqlEndpoint: SparqlEndpoint,
                        inputTask: Option[Task[_ <: TaskSpec]],
                        outputTask: Option[Task[_ <: TaskSpec]],
                        limit: Int,
                        executionReportUpdater: Option[SparqlSelectExecutionReportUpdater])
                       (implicit pluginContext: PluginContext): CloseableIterator[Entity] = {
    implicit val user: UserContext = pluginContext.user
    val selectLimit = math.min(sparqlSelectTask.intLimit.getOrElse(Integer.MAX_VALUE), limit)
    val taskProperties = TaskProperties.create(inputTask, outputTask, pluginContext)
    val templateVariables = pluginContext.templateVariables.all.variables
    val query = sparqlSelectTask.queryTemplate.generate(None, taskProperties, templateVariables).head
    val results = executeSelect(sparqlEndpoint, query, selectLimit, Some(sparqlSelectTask.sparqlTimeout))
    val vars: IndexedSeq[String] = getSparqlVars(sparqlSelectTask)
    createEntities(sparqlSelectTask, results.bindings, vars, executionReportUpdater)
  }

  /**
   * Executes the select query on the SPARQL endpoint.
   *
   * @param query   The SELECT query to execute
   * @param limit   The max. number of rows to fetch
   * @param timeout An optional timeout in ms for the query execution. If defined it should have an positive value, else it will be ignored.
   *                This timeout is passed to the underlying SPARQL endpoint implementation.
   */
  private def executeSelect(sparqlEndpoint: SparqlEndpoint,
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

  private def getSparqlVars(taskData: SparqlSelectCustomTask): IndexedSeq[String] = {
    val vars = taskData.outputSchema.typedPaths map { v =>
      v.propertyUri match {
        case Some(prop) =>
          prop.uri
        case _ =>
          throw TaskException("Path in input schema of SPARQL select operator is not a simple forward property: " + v.toUntypedPath.normalizedSerialization)
      }
    }
    vars
  }

  private def createEntities(taskData: SparqlSelectCustomTask,
                             bindings: CloseableIterator[SortedMap[String, RdfNode]],
                             vars: IndexedSeq[String],
                             executionReportUpdater: Option[SparqlSelectExecutionReportUpdater]): CloseableIterator[Entity] = {
    implicit val prefixes: Prefixes = Prefixes.empty
    var schemaReported = false
    val increase: Entity => Unit = (entity: Entity) => executionReportUpdater match {
      case Some(updater) =>
        if (!schemaReported) {
          schemaReported = true
          updater.startNewOutputSamples(entity.schema)
        }
        updater.addEntityAsSampleEntity(entity)
        updater.increaseEntityCounter()
      case None => // no-op
    }

    var count = 0
    val entityIterator = bindings.map { binding =>
      count += 1
      val values = vars map { v =>
        binding.get(v).toSeq.map(_.value)
      }
      val entity = Entity(DataSource.URN_NID_PREFIX + count, values = values, schema = taskData.outputSchema)
      increase(entity)
      entity
    }
    entityIterator.thenClose(() => executionReportUpdater.foreach(updater => updater.executionDone()))
  }
}

case class SparqlSelectExecutionReportUpdater(task: Task[TaskSpec],
                                              context: ActivityContext[ExecutionReport]) extends ExecutionReportUpdater {

  override def operationLabel: Option[String] = Some("generate queries")

  override def entityLabelSingle: String = "Row"

  override def entityLabelPlural: String = "Rows"

  override def minEntitiesBetweenUpdates: Int = 1
}