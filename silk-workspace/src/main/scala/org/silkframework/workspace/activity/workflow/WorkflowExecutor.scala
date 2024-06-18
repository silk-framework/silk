package org.silkframework.workspace.activity.workflow

import org.silkframework.config.{Prefixes, Task, TaskSpec}
import org.silkframework.dataset.Dataset
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.execution._
import org.silkframework.runtime.activity.Status.Canceling
import org.silkframework.runtime.activity._
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Identifier
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.activity.workflow.ReconfigureTasks.ReconfigurableTask

import scala.collection.mutable
import scala.util.control.NonFatal

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

  /**
    * Executes a workflow operator.
    *
    * @param operation The operation, e.g., "reading"
    * @param nodeId The workflow node identifier
    * @param task The task to be executed
    * @param inputs Inputs
    * @param output Output definition
    * @param workflowRunContext
    * @param prefixes
    * @tparam TaskType
    * @return
    */
  protected def execute[TaskType <: TaskSpec](operation: String,
                                              nodeId: Identifier,
                                              task: Task[TaskType],
                                              inputs: Seq[ExecType#DataType],
                                              output: ExecutorOutput)
                                             (implicit workflowRunContext: WorkflowRunContext, prefixes: Prefixes): Option[ExecType#DataType] = {
    implicit val pluginContext: PluginContext = PluginContext.fromProject(project)(workflowRunContext.userContext)
    val taskContext = workflowRunContext.taskContext(nodeId, task)
    updateProgress(operation, task)
    val result =
      try {
        ExecutorRegistry.execute(task, inputs, output, executionContext, taskContext)
      } catch {
        case NonFatal(ex) =>
          workflowRunContext.activityContext.value.updateWith(_.addFailedNode(nodeId, ex))
          throw ex
      }
    for(error <- taskContext.value.get.flatMap(_.error)) {
      val ex = WorkflowExecutionException(error)
      workflowRunContext.activityContext.value.updateWith(_.addFailedNode(nodeId, ex))
      throw ex
    }
    result
  }

  protected def executeAndClose[TaskType <: TaskSpec, ResultType](operation: String,
                                                                  nodeId: Identifier,
                                                                  task: Task[TaskType],
                                                                  inputs: Seq[ExecType#DataType],
                                                                  output: ExecutorOutput)
                                                                 (process: Option[ExecType#DataType] => ResultType)
                                                                 (implicit workflowRunContext: WorkflowRunContext, prefixes: Prefixes): ResultType = {
    val result = execute(operation, nodeId, task, inputs, output)
    try {
      process(result)
    } finally {
      for(r <- result) {
        r.close()
      }
    }
  }

  /**
    * Update the progress and write a log message.
    *
    * @param operation Operation, e.g., "reading"
    * @param task task that is executed
    * @param workflowRunContext Workflow Context
    */
  protected def updateProgress(operation: String, task: Task[_ >: TaskSpec])(implicit workflowRunContext: WorkflowRunContext): Unit = {
    val taskLabel = task.fullLabel
    val progress = (workflowRunContext.alreadyExecuted.size.toDouble + 1) / (workflowNodes.size + 1)
    workflowRunContext.activityContext.status.update(s"$operation '$taskLabel'", progress)
  }

  /** Make sure that the workflow does not try to write into a read-only dataset. */
  protected def checkReadOnlyDatasets()
                                     (implicit userContext: UserContext): Unit = {
    val readOnlyDatasetsAsOutputs = currentWorkflow.outputDatasets(project).filter(_.readOnly)
    if(readOnlyDatasetsAsOutputs.nonEmpty) {
      throw WorkflowExecutionException("Workflow execution is not allowed to start because following read-only datasets would be written into: " +
        readOnlyDatasetsAsOutputs.map(_.fullLabel).mkString("'", "', '", "'"))
    }
  }

  /** Return error if legacy VariableDataset has no replacement. Marked variable datasets do not need to be replaced. */
  protected def checkVariableDatasets()
                                     (implicit userContext: UserContext): Unit = {
    val variableDatasets = currentWorkflow.legacyVariableDatasets(project)
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
        throw WorkflowExecutionException(s"No dataset task found in project ${project.id} with id " + datasetTaskId)
    }
  }

  /** Computes the entities for a specific workflow node
    *
    * @param workflowDependencyNode The workflow node for which entities should be computed.
    * @param outputTask             The output task for the workflow node.
    */
  protected def workflowNodeEntities[T](workflowDependencyNode: WorkflowDependencyNode,
                                        outputTask: Task[_ <: TaskSpec])
                                       (process: Option[EntityHolder] => T)
                                       (implicit workflowRunContext: WorkflowRunContext): T

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
    implicit val pluginContext: PluginContext = PluginContext.fromProject(project)(workflowRunContext.userContext)
    try {
      workflowRunContext.reconfiguredTasks.getOrElseUpdate(
        workflowNode.workflowNode, {
          // Calculate the parameters
          val configInputEntities = workflowNode.configInputNodes.flatMap(node => workflowNodeEntities(node, task) { entities => entities.flatMap(_.headOption) })
          task.reconfigure(configInputEntities)
        }
      ).asInstanceOf[Task[T]]
    } catch {
      case ex: ValidationException =>
        throw new ValidationException(s"Failed to re-configure task '${task.label()}'. Error details: " + ex.getMessage)
    }
  }

  protected def task(workflowDependencyNode: WorkflowDependencyNode)
                    (implicit workflowRunContext: WorkflowRunContext): Task[_ <: TaskSpec] = {
    implicit val userContext: UserContext = workflowRunContext.userContext
    val taskId = workflowDependencyNode.workflowNode.task
    project.anyTaskOption(taskId) match {
      case Some(task) =>
        reconfigureTask(workflowDependencyNode, task)
      case None =>
        throw WorkflowExecutionException(s"No task found in project ${project.id} with id " + taskId)
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
  private val reportListeners: mutable.Buffer[TaskReportListener] = mutable.Buffer.empty

  /**
   * Listeners for updates to the workflow status.
   * We need to hold them to prevent their garbage collection.
   */
  private val statusListeners: mutable.Buffer[WorkflowStatusListener] = mutable.Buffer.empty

  /** Creates an activity context for a specific task that will be executed in the workflow.
    * Also wires the task execution report to the workflow execution report. */
  def taskContext(nodeId: Identifier, task: Task[_ <: TaskSpec])(implicit workflowRunContext: WorkflowRunContext): ActivityContext[ExecutionReport] = {
    val projectAndTaskString = activityContext.status.projectAndTaskId.map(ids => ids.copy(ids.projectId, ids.taskId.map(_ + " -> " + task.id)))
    val taskContext = new ActivityMonitor[ExecutionReport](task.id, Some(activityContext), projectAndTaskId = projectAndTaskString)
    listenForTaskReports(nodeId, task, taskContext)
    listenForWorkflowCancellation(taskContext, workflowRunContext.activityContext.status)
    taskContext
  }

  // Creates a task report listener that will add that task report to the overall workflow report
  private def listenForTaskReports(nodeId: Identifier,
                                   task: Task[_ <: TaskSpec],
                                   taskContext: ActivityMonitor[ExecutionReport]): Unit = {
    // Add initial task report
    activityContext.value.updateWith(_.addReport(nodeId, SimpleExecutionReport.initial(task)))
    // Listen for changes and update the task report for each change
    val listener = new TaskReportListener(reportListeners.size, nodeId)
    taskContext.value.subscribe(listener)
    reportListeners.append(listener)
  }

  // Listens for updates to the workflow status
  private def listenForWorkflowCancellation(taskContext: ActivityMonitor[ExecutionReport], workflowStatus: Observable[Status]): Unit = {
    val listener = new WorkflowStatusListener(taskContext.status)
    workflowStatus.subscribe(listener)
    statusListeners.append(listener)
  }

  /**
    * Updates the workflow execution report on each update of a task report.
    */
  private class TaskReportListener(index: Int, nodeId: Identifier) extends (ExecutionReport => Unit) {
    def apply(report: ExecutionReport): Unit = activityContext.value.synchronized {
      activityContext.value.updateWith(_.updateReport(index, nodeId, report))
    }
  }

  private class WorkflowStatusListener(taskStatus: StatusHolder) extends (Status => Unit) {
    override def apply(workflowStatus: Status): Unit = {
      if(workflowStatus.isInstanceOf[Canceling]) {
        taskStatus.synchronized {
          if(taskStatus().isRunning) {
            taskStatus.update(Canceling(None), logStatus = false)
          }
        }
      }
    }
  }
}

/** When thrown from a workflow task inside a workflow, this will lead to the immediate stop of the workflow execution.
  *
  * @param msg          Cancellation message
  * @param failWorkflow If true, the workflow execution will fail. If false, the workflow execution will be considered successfull.
  **/
case class StopWorkflowExecutionException(msg: String, failWorkflow: Boolean) extends Exception(msg)