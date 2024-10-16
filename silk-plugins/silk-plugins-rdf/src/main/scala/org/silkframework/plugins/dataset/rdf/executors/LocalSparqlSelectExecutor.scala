package org.silkframework.plugins.dataset.rdf.executors

import org.silkframework.config.{Prefixes, Task, TaskSpec}
import org.silkframework.dataset.DataSource
import org.silkframework.dataset.rdf.{SparqlEndpoint, SparqlResults}
import org.silkframework.entity.Entity
import org.silkframework.execution.local.{GenericEntityTable, LocalEntities, LocalExecution, LocalExecutor}
import org.silkframework.execution.typed.SparqlEndpointEntitySchema
import org.silkframework.execution.{ExecutionReport, ExecutionReportUpdater, ExecutorOutput, TaskException}
import org.silkframework.plugins.dataset.rdf.tasks.SparqlSelectCustomTask
import org.silkframework.runtime.activity.{ActivityContext, UserContext}
import org.silkframework.runtime.iterator.CloseableIterator
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
    implicit val user: UserContext = pluginContext.user

    inputs match {
      case Seq(SparqlEndpointEntitySchema(sparql)) =>
        implicit val executionReportUpdater: SparqlSelectExecutionReportUpdater = SparqlSelectExecutionReportUpdater(task, context)
        val entities = executeOnSparqlEndpoint(taskData, sparql.task.data.plugin.sparqlEndpoint, executionReportUpdater = Some(executionReportUpdater))
        Some(GenericEntityTable(entities, entitySchema = taskData.outputSchema, task))
      case _ =>
        throw TaskException("SPARQL select executor did not receive a SPARQL endpoint as requested!")
    }
  }

  def executeOnSparqlEndpoint(sparqlSelectTask: SparqlSelectCustomTask,
                              sparql: SparqlEndpoint,
                              limit: Int = Integer.MAX_VALUE,
                              executionReportUpdater: Option[SparqlSelectExecutionReportUpdater])
                             (implicit userContext: UserContext): CloseableIterator[Entity] = {
    val selectLimit = math.min(sparqlSelectTask.intLimit.getOrElse(Integer.MAX_VALUE), limit)
    val results = select(sparqlSelectTask, sparql, selectLimit)
    val vars: IndexedSeq[String] = getSparqlVars(sparqlSelectTask)
    createEntities(sparqlSelectTask, results, vars, executionReportUpdater)
  }

  private def select(sparqlSelectTask: SparqlSelectCustomTask, sparql: SparqlEndpoint, selectLimit: Int)
                    (implicit userContext: UserContext): SparqlResults = {
    executeSelect(sparql, sparqlSelectTask.selectQuery.str, selectLimit, Some(sparqlSelectTask.sparqlTimeout))
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
    val increase: Entity => Unit = (entity: Entity) => executionReportUpdater match {
      case Some(updater) => () =>
        if (!schemaReported) {
          schemaReported = true
          updater.startNewOutputSamples(entity.schema)
        }
        updater.addSampleEntity(entity)
        updater.increaseEntityCounter()
      case None => () => {} // no-op
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