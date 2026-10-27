package org.silkframework.workspace.activity.workflow

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.execution._
import org.silkframework.runtime.activity.Status.Canceling
import org.silkframework.runtime.activity._
import org.silkframework.runtime.templating.{ExecutionVariablesHolder, TemplateVariables}
import org.silkframework.util.{Identifier, Timer}

import java.time.{Duration, Instant}
import scala.collection.mutable

/**
 * A context for a single workflow execution.
 *
 * @param activityContext The activity context for the workflow execution.
 * @param workflow The workflow that is being be executed.
 * @param userContext The user that is executing the workflow.
 * @param alreadyExecuted The workflow nodes that have already been executed.
 * @param reconfiguredTasks The already tasks that have been reconfigured.
 * @param workflowVariables The execution variables for this run (defaults of the workflow, overridden by the variables provided at start).
 * @param taskExecutors The executors for each task by task id.
 * @param parentExecutionVariablesHolder The enclosing run's execution-variable holder, if this workflow runs as an operator inside another workflow.
 */
case class WorkflowRunContext(activityContext: ActivityContext[WorkflowExecutionReport],
                              workflow: Workflow,
                              userContext: UserContext,
                              alreadyExecuted: mutable.Set[WorkflowNode] = mutable.Set(),
                              reconfiguredTasks: mutable.Map[WorkflowNode, Task[_ <: TaskSpec]] = mutable.Map(),
                              workflowVariables: TemplateVariables = TemplateVariables.empty,
                              taskExecutors: mutable.Map[Identifier, Executor[_, _]] = mutable.Map(),
                              parentExecutionVariablesHolder: Option[ExecutionVariablesHolder] = None) {

  /**
    * Shared, thread-safe holder for execution-scope variables. Mutated by plugin code during execution.
    * Seeded with the execution variables for this run: the workflow's execution variables as defaults,
    * overridden by the variables provided when the run was started.
    * Nested workflow runs share the enclosing run's holder.
    */
  val executionVariablesHolder: ExecutionVariablesHolder =
    parentExecutionVariablesHolder.getOrElse(new ExecutionVariablesHolder(workflowVariables))

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

  /**
    * The operations in progress, innermost first, e.g. "Executing 'My task'". An operation is in progress until the
    * result of its task has been consumed, so a task whose entities are still being pulled by a later task counts as well.
    */
  @volatile
  private var runningOperations: List[String] = Nil

  /** Runs an operation on a task, e.g. "Executing 'My task'", and tracks it as in progress until it returns. */
  def trackOperation[T](operation: String)(body: => T): T = {
    runningOperations = operation :: runningOperations
    try {
      body
    } finally {
      runningOperations = runningOperations.drop(1)
    }
  }

  /**
    * Updates the workflow execution report. The report is also updated by the task report listeners and, on
    * cancellation, by the cancelling thread, so every update must take the lock of the report value.
    */
  def updateReport(update: WorkflowExecutionReport => WorkflowExecutionReport): Unit = activityContext.value.synchronized {
    activityContext.value.updateWith(update)
  }

  // Set when the workflow is cancelled while tasks are running: these tasks (as listed in the warning) and the time of the cancel.
  // Guarded by the lock of the report value, as is 'finished', so that the cancelling thread and the workflow thread see the same state.
  private var cancelledWhileRunning: Option[(String, Instant)] = None

  // Set once the run has ended, so that a cancel arriving at that moment does not record tasks as still running.
  private var finished = false

  private val stopsOnceFinished = "The workflow stops once they have finished."

  private def runningTasksWarning(tasks: String, outcome: String): String = {
    s"The workflow was cancelled while the following tasks were still running: $tasks. $outcome"
  }

  // Records the operations that are still in progress when the workflow is cancelled. Held in a val, as subscriptions are weak.
  private val cancellationListener: Status => Unit = { status =>
    if(status.isInstanceOf[Canceling]) {
      activityContext.value.synchronized {
        val running = runningOperations.reverse.distinct // A task may be in progress twice, e.g. during its error output pass
        if(!finished && running.nonEmpty) {
          val tasks = running.mkString(", ")
          cancelledWhileRunning = Some((tasks, Instant.now()))
          updateReport(_.addWarning(runningTasksWarning(tasks, stopsOnceFinished)))
        }
      }
    }
  }
  activityContext.status.subscribe(cancellationListener)

  /**
    * Marks the end of the run. If the workflow was cancelled while tasks were still running, the warning is updated
    * with the time these tasks took to finish, which tells a blocking task apart from a cancel that went through at once.
    */
  def runFinished(): Unit = activityContext.value.synchronized {
    finished = true
    for((tasks, cancelledAt) <- cancelledWhileRunning) {
      val finishedAfter = s"They finished after ${Timer.formatDuration(Duration.between(cancelledAt, Instant.now()).toMillis)}."
      updateReport(_.replaceWarning(runningTasksWarning(tasks, stopsOnceFinished), runningTasksWarning(tasks, finishedAfter)))
      cancelledWhileRunning = None
    }
  }

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
    val listener = activityContext.value.synchronized {
      // Add initial task report
      activityContext.value.updateWith(_.addReport(nodeId, SimpleExecutionReport.initial(task)))
      new TaskReportListener(activityContext.value().taskReports.size - 1, nodeId)
    }
    // Listen for changes and update the task report for each change
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
    * A node may run multiple executions within one workflow run (e.g. a dataset node executes one
    * write per input): each execution ends with a report marked as done, so an update arriving
    * after that starts a new report entry instead of overwriting the finished one.
    */
  private class TaskReportListener(private var index: Int, nodeId: Identifier) extends (ExecutionReport => Unit) {
    def apply(report: ExecutionReport): Unit = activityContext.value.synchronized {
      if(activityContext.value().taskReports(index).report.isDone) {
        activityContext.value.updateWith(_.addReport(nodeId, report))
        index = activityContext.value().taskReports.size - 1
      } else {
        activityContext.value.updateWith(_.updateReport(index, nodeId, report))
      }
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
