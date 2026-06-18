package org.silkframework.workspace.activity.workflow

import org.silkframework.config.TaskSpec
import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.util.Identifier
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.activity.{ActivityLimit, ActivityLimiterKey, WorkspaceActivityFactory}

@Plugin(
  id = "WorkflowExecutionActivityLimit",
  label = "Workflow execution activity limit"
)
case class WorkflowExecutionActivityLimit() extends ActivityLimit {

  private val guardedActivityIds = Set("ExecuteLocalWorkflow", "ExecuteSparkWorkflow", "ExecuteWorkflowWithPayload")

  override def limitFor(task: Option[ProjectTask[_ <: TaskSpec]], factory: WorkspaceActivityFactory): Option[Int] = {
    task.collect {
      case workflowTask if workflowTask.data.isInstanceOf[Workflow] && guardedActivityIds.contains(factory.pluginSpec.id.toString) =>
        workflowTask.data.asInstanceOf[Workflow].maxParallelExecutions.value
    }.flatten
  }

  override def limiterKey(projectId: Option[Identifier],
                          taskId: Option[Identifier],
                          task: Option[ProjectTask[_ <: TaskSpec]],
                          factory: WorkspaceActivityFactory): ActivityLimiterKey = {
    ActivityLimiterKey(projectId, taskId, limitId = "workflow-execution")
  }

  override def waitingMessage(task: Option[ProjectTask[_ <: TaskSpec]], factory: WorkspaceActivityFactory): String = {
    task.map(workflowTask => s"Waiting for workflow execution slot of '${workflowTask.id}'").getOrElse("Waiting for workflow execution slot")
  }
}
