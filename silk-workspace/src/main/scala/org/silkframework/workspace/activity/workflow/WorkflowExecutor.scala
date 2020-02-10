package org.silkframework.workspace.activity.workflow

import org.silkframework.config.{PlainTask, Prefixes, Task, TaskSpec}
import org.silkframework.dataset.Dataset
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.entity.Entity
import org.silkframework.execution._
import org.silkframework.runtime.activity._
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Identifier
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
                                              output: ExecutorOutput)
                                             (implicit workflowRunContext: WorkflowRunContext, prefixes: Prefixes): Option[ExecType#DataType] = {
    implicit val userContext: UserContext = workflowRunContext.userContext
    ExecutorRegistry.execute(task, inputs, output, executionContext, workflowRunContext.taskContext(task.id))
  }

  /** Return error if VariableDataset is used in output and input */
  protected def checkVariableDatasets()
                                     (implicit userContext: UserContext): Unit = {
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

  protected def datasetTask(workflowDataset: WorkflowDependencyNode)
                           (implicit workflowRunContext: WorkflowRunContext): Task[GenericDatasetSpec] = {
    val datasetTaskId = workflowDataset.workflowNode.task
    implicit val userContext: UserContext = workflowRunContext.userContext
    project.taskOption[GenericDatasetSpec](datasetTaskId) match {
      case Some(datasetTask) =>
        reconfigureTask(workflowDataset, datasetTask)
      case None =>
        throw WorkflowException(s"No dataset task found in project ${project.name} with id " + datasetTaskId)
    }
  }

  /** Computes the entities for a specific workflow node
    *
    * @param workflowDependencyNode The workflow node for which entities should be computed.
    * @param outputTask             The output task for the workflow node.
    */
  protected def workflowNodeEntities(workflowDependencyNode: WorkflowDependencyNode,
                                     outputTask: Task[_ <: TaskSpec])
                                    (implicit workflowRunContext: WorkflowRunContext): Option[EntityHolder]

  /**
    * Fetches the re-configuration parameters for a workflow node.
    * For each input it takes the first entity and for each property the first value to create property -> value pairs.
    * The property should match the property name of the config parameter of the task it should override.
    *
    * @param workflowNode The workflow node that should be re-configured
    * @param task         The task that should be re-configured.
    */
  private def reconfigureTask[T <: TaskSpec](workflowNode: WorkflowDependencyNode,
                                             task: Task[T])
                                            (implicit workflowRunContext: WorkflowRunContext): Task[T] = {
    implicit val prefixes: Prefixes = Prefixes.empty // TODO: propagate prefixes?
    try {
      workflowRunContext.reconfiguredTasks.getOrElseUpdate(
        workflowNode.workflowNode, {
          // Calculate the parameters
          val configInputEntities = workflowNode.configInputNodes.flatMap(node => workflowNodeEntities(node, task))
          // Merge parameters, config parameters of later inputs overwrite those of earlier inputs
          val configParameters = configInputEntities.
              flatMap(_.headOption.map(extractConfigParameterMap)).
              foldLeft(Map.empty[String, String])(_ ++ _)
          if (configParameters.isEmpty) {
            task
          } else {
            implicit val resourceManager = workflowTask.project.resources
            PlainTask(id = task.id, data = task.data.withProperties(configParameters), metaData = task.metaData)
          }
        }
      ).asInstanceOf[Task[T]]
    } catch {
      case ex: ValidationException =>
        throw new ValidationException(s"Failed to re-configure task '${task.taskLabel()}'. Error details: " + ex.getMessage)
    }
  }

  private def extractConfigParameterMap(entity: Entity): Map[String, String] = {
    val configPairs = for((propertyName, multiValue) <- entity.schema.propertyNames.zip(entity.values);
                          value <- multiValue.headOption) yield {
      propertyName -> value
    }
    configPairs.toMap
  }

  protected def task(workflowDependencyNode: WorkflowDependencyNode)
                    (implicit workflowRunContext: WorkflowRunContext): Task[_ <: TaskSpec] = {
    implicit val userContext: UserContext = workflowRunContext.userContext
    val taskId = workflowDependencyNode.workflowNode.task
    project.anyTaskOption(taskId) match {
      case Some(task) =>
        reconfigureTask(workflowDependencyNode, task)
      case None =>
        throw WorkflowException(s"No task found in project ${project.name} with id " + taskId)
    }
  }

  /** Necessary update for the user context, so external datasets can be accessed in safe-mode inside a workflow execution. */
  def updateUserContext(userContext: UserContext): UserContext = {
    val executionContext = userContext.executionContext
    val updatedUserContext = userContext.withExecutionContext(executionContext.copy(insideWorkflow = true))
    updatedUserContext
  }
}

case class WorkflowRunContext(activityContext: ActivityContext[WorkflowExecutionReport],
                              workflow: Workflow,
                              userContext: UserContext,
                              alreadyExecuted: mutable.Set[WorkflowNode] = mutable.Set(),
                              reconfiguredTasks: mutable.Map[WorkflowNode, Task[_ <: TaskSpec]] = mutable.Map()) {
  /**
    * Listeners for updates to task reports.
    * We need to hold them to prevent their garbage collection.
    */
  private var reportListeners: List[TaskReportListener] = List.empty

  /** Creates an activity context for a specific task that will be executed in the workflow.
    * Also wires the task execution report to the workflow execution report. */
  def taskContext(taskId: Identifier): ActivityContext[ExecutionReport] = {
    val projectAndTaskString = activityContext.status.projectAndTaskId.map(ids => ids.copy(ids.projectId, ids.taskId.map(_ + " -> " + taskId)))
    val taskContext = new ActivityMonitor[ExecutionReport](taskId, Some(activityContext), projectAndTaskId = projectAndTaskString)
    listenForTaskReports(taskId, taskContext)
    taskContext
  }

  // Creates a task report listener that will add that task report to the overall workflow report
  private def listenForTaskReports(taskId: Identifier,
                                   taskContext: ActivityMonitor[ExecutionReport]): Unit = {
    val listener = new TaskReportListener(taskId)
    taskContext.value.subscribe(listener)
    reportListeners ::= listener
  }

  /**
    * Updates the workflow execution report on each update of a task report.
    */
  private class TaskReportListener(task: Identifier) extends (ExecutionReport => Unit) {
    def apply(report: ExecutionReport): Unit = activityContext.value.synchronized {
      activityContext.value() = activityContext.value().withReport(task, report)
    }
  }
}
