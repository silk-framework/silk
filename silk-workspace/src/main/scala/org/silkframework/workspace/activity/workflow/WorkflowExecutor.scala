package org.silkframework.workspace.activity.workflow

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.dataset.Dataset
import org.silkframework.entity.EntitySchema
import org.silkframework.execution.{ExecutionReport, ExecutionType, ExecutorRegistry}
import org.silkframework.runtime.activity.{Activity, ActivityContext, ActivityMonitor}
import org.silkframework.workspace.ProjectTask

import scala.collection.mutable

/**
  * Created by robert on 9/21/2016.
  */
trait WorkflowExecutor[ExecType <: ExecutionType] extends Activity[WorkflowExecutionReport] {

  /** Returns the workflow task */
  protected def workflowTask: ProjectTask[Workflow]

  /** Returns a map of datasets that can replace variable datasets used as data sources in a workflow */
  protected def replaceDataSources: Map[String, Dataset]

  protected def executionContext: ExecType

  /** Returns a map of datasets that can replace variable datasets used as data sinks in a workflow */
  protected def replaceSinks: Map[String, Dataset]

  protected def currentWorkflow = workflowTask.data

  protected def project = workflowTask.project
  protected def workflowNodes = currentWorkflow.nodes

  protected def execute[TaskType <: TaskSpec](task: Task[TaskType],
                                              inputs: Seq[ExecType#DataType],
                                              outputSchema: Option[EntitySchema])
                                             (implicit workflowRunContext: WorkflowRunContext): Option[ExecType#DataType] = {
    val activity = workflowRunContext.activityContext
    val monitor = new ActivityMonitor[ExecutionReport](task.id, Some(activity))
    val result = ExecutorRegistry.execute(task, inputs, outputSchema, executionContext, monitor)
    if (monitor.value.isDefined) {
      activity.value() = activity.value().withReport(task.id, monitor.value())
    }
    result
  }

  /** Return error if VariableDataset is used in output and input */
  protected def checkVariableDatasets(): Unit = {
    val variableDatasets = currentWorkflow.variableDatasets(project)
    val notCoveredVariableDatasets = variableDatasets.dataSources.filter(!replaceDataSources.contains(_))
    if (notCoveredVariableDatasets.nonEmpty) {
      throw new scala.IllegalArgumentException("No replacement for following variable datasets as data sources provided: " +
          notCoveredVariableDatasets.mkString(", "))
    }
    val notCoveredVariableSinks = variableDatasets.sinks.filter(!replaceSinks.contains(_))
    if (notCoveredVariableSinks.nonEmpty) {
      throw new scala.IllegalArgumentException("No replacement for following variable datasets as data sinks provided: " +
          notCoveredVariableSinks.mkString(", "))
    }
  }

  protected def workflow(implicit workflowRunContext: WorkflowRunContext): Workflow = workflowRunContext.workflow

  case class WorkflowRunContext(activityContext: ActivityContext[WorkflowExecutionReport],
                                workflow: Workflow,
                                alreadyExecuted: mutable.Set[WorkflowNode] = mutable.Set())

}
