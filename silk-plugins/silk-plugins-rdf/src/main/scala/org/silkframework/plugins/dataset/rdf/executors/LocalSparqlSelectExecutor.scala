package org.silkframework.plugins.dataset.rdf.executors

import org.silkframework.config.{Prefixes, Task, TaskSpec}
import org.silkframework.dataset.{DataSource, DatasetSpec}
import org.silkframework.dataset.rdf.{RdfDataset, SparqlEndpoint, SparqlResults}
import org.silkframework.entity.Entity
import org.silkframework.execution.local.{GenericEntityTable, LocalEntities, LocalExecution, LocalExecutor}
import org.silkframework.execution.typed.SparqlEndpointEntitySchema
import org.silkframework.execution.{ExecutionReport, ExecutionReportUpdater, ExecutorOutput, TaskException}
import org.silkframework.plugins.dataset.rdf.tasks.SparqlSelectCustomTask
import org.silkframework.plugins.dataset.rdf.tasks.templating.TaskProperties
import org.silkframework.runtime.activity.{ActivityContext, UserContext}
import org.silkframework.runtime.iterator.{AbstractRewindableEntityIterator, CloseableIterator}
import org.silkframework.runtime.plugin.PluginContext

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

    inputs match {
      case Seq(SparqlEndpointEntitySchema(sparql)) =>
        implicit val executionReportUpdater: SparqlSelectExecutionReportUpdater = SparqlSelectExecutionReportUpdater(task, context)
        val entities = new LocalSparqlSelectIterator(taskData, sparql.task, output.task, executionReportUpdater = Some(executionReportUpdater))
        Some(GenericEntityTable(entities, entitySchema = taskData.outputSchema, task))
      case _ =>
        throw TaskException("SPARQL select executor did not receive a SPARQL endpoint as requested!")
    }
  }
}

/**
 * Iterator that executes a SPARQL SELECT query on a SPARQL endpoint and returns the results as entities.
 */
class LocalSparqlSelectIterator(sparqlSelectTask: SparqlSelectCustomTask,
                                inputTask: Task[DatasetSpec[RdfDataset]],
                              outputTask: Option[Task[_ <: TaskSpec]],
                                limit: Int = Integer.MAX_VALUE,
                                executionReportUpdater: Option[SparqlSelectExecutionReportUpdater])
                               (implicit pluginContext: PluginContext) extends AbstractRewindableEntityIterator {

  override def newIterator(): CloseableIterator[Entity] = {
    val selectLimit = math.min(sparqlSelectTask.intLimit.getOrElse(Integer.MAX_VALUE), limit)
    val results = select(sparqlSelectTask, inputTask, outputTask, selectLimit)
    val vars: IndexedSeq[String] = getSparqlVars(sparqlSelectTask)
    createEntities(sparqlSelectTask, results, vars, executionReportUpdater)
  }

  private def select(sparqlSelectTask: SparqlSelectCustomTask,
                     inputTask: Task[_ <: DatasetSpec[RdfDataset]],
                     outputTask: Option[Task[_ <: TaskSpec]],
                     selectLimit: Int)
                    (implicit pluginContext: PluginContext): SparqlResults = {
    implicit val user: UserContext = pluginContext.user
    val sparqlEndpoint = inputTask.data.plugin.sparqlEndpoint
    val taskProperties = TaskProperties.create(Some(inputTask), outputTask, pluginContext)
    executeSelect(sparqlEndpoint, sparqlSelectTask.queryTemplate.generate(Map.empty, taskProperties), selectLimit, Some(sparqlSelectTask.sparqlTimeout))
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
                             results: SparqlResults,
                             vars: IndexedSeq[String],
                             executionReportUpdater: Option[SparqlSelectExecutionReportUpdater]): CloseableIterator[Entity] = {
    implicit val prefixes: Prefixes = Prefixes.empty
    var schemaReported = false
    val increase: (Entity => Unit) = (entity: Entity) => executionReportUpdater match {
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
    val entityIterator = results.bindings.map { binding =>
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