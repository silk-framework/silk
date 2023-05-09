package org.silkframework.plugins.dataset.rdf.executors

import org.silkframework.config.{Prefixes, Task, TaskSpec}
import org.silkframework.dataset.DataSource
import org.silkframework.dataset.rdf.{SparqlEndpointEntityTable, SparqlResults}
import org.silkframework.entity.Entity
import org.silkframework.execution.local.{GenericEntityTable, LocalEntities, LocalExecution, LocalExecutor}
import org.silkframework.execution.{ExecutionReport, ExecutionReportUpdater, ExecutorOutput, TaskException}
import org.silkframework.plugins.dataset.rdf.tasks.SparqlSelectCustomTask
import org.silkframework.runtime.activity.{ActivityContext, UserContext}
import org.silkframework.runtime.iterator.{CloseableIterator, LegacyTraversable}

/**
  * Local executor for [[SparqlSelectCustomTask]].
  */
case class LocalSparqlSelectExecutor() extends LocalExecutor[SparqlSelectCustomTask] {
  override def execute(task: Task[SparqlSelectCustomTask],
                       inputs: Seq[LocalEntities],
                       output: ExecutorOutput,
                       execution: LocalExecution,
                       context: ActivityContext[ExecutionReport])
                      (implicit userContext: UserContext, prefixes: Prefixes): Option[LocalEntities] = {
    val taskData = task.data

    inputs match {
      case Seq(sparql: SparqlEndpointEntityTable) =>
        implicit val executionReportUpdater: SparqlSelectExecutionReportUpdater = SparqlSelectExecutionReportUpdater(task, context)
        val entities = executeOnSparqlEndpointEntityTable(taskData, sparql, executionReportUpdater = Some(executionReportUpdater))
        Some(GenericEntityTable(entities, entitySchema = taskData.outputSchema, task))
      case _ =>
        throw TaskException("SPARQL select executor did not receive a SPARQL endpoint as requested!")
    }
  }

  def executeOnSparqlEndpointEntityTable(sparqlSelectTask: SparqlSelectCustomTask,
                                         sparql: SparqlEndpointEntityTable,
                                         limit: Int = Integer.MAX_VALUE,
                                         executionReportUpdater: Option[SparqlSelectExecutionReportUpdater])
                                        (implicit userContext: UserContext): CloseableIterator[Entity] = {
    val selectLimit = math.min(sparqlSelectTask.intLimit.getOrElse(Integer.MAX_VALUE), limit)
    val results = select(sparqlSelectTask, sparql, selectLimit)
    val vars: IndexedSeq[String] = getSparqlVars(sparqlSelectTask)
    createEntities(sparqlSelectTask, results, vars, executionReportUpdater)
  }

  private def select(sparqlSelectTask: SparqlSelectCustomTask, sparql: SparqlEndpointEntityTable, selectLimit: Int)
                    (implicit userContext: UserContext): SparqlResults = {
    sparql.select(sparqlSelectTask.selectQuery.str, selectLimit, Some(sparqlSelectTask.sparqlTimeout))
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
    val increase: () => Unit = executionReportUpdater match {
      case Some(updater) => () =>
        updater.increaseEntityCounter()
      case None => () => {} // no-op
    }
    new LegacyTraversable[Entity] {
      override def foreach[U](f: Entity => U): Unit = {
        var count = 0
        results.bindings foreach { binding =>
          count += 1
          val values = vars map { v =>
            binding.get(v).toSeq.map(_.value)
          }
          f(Entity(DataSource.URN_NID_PREFIX + count, values = values, schema = taskData.outputSchema))
          increase()
        }
        executionReportUpdater.foreach(updater => updater.executionDone())
      }
    }
  }
}

case class SparqlSelectExecutionReportUpdater(task: Task[TaskSpec],
                                              context: ActivityContext[ExecutionReport]) extends ExecutionReportUpdater {

  override def operationLabel: Option[String] = Some("generate queries")

  override def entityLabelSingle: String = "Row"

  override def entityLabelPlural: String = "Rows"

  override def minEntitiesBetweenUpdates: Int = 1
}