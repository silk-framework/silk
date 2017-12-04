package org.silkframework.workspace.activity.workflow

import org.silkframework.runtime.activity.Activity
import org.silkframework.runtime.plugin.Plugin
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.activity.TaskActivityFactory

/**
  * A factory that creates a local workflow executor.
  */
@Plugin(
  id = "ExecuteLocalWorkflow",
  label = "Execute local Workflow",
  categories = Array("Workflow"),
  description = "Executes the workflow locally."
)
case class LocalWorkflowExecutorFactory() extends TaskActivityFactory[Workflow, LocalWorkflowExecutor] {

  override def apply(task: ProjectTask[Workflow]): Activity[WorkflowExecutionReport] = {
    LocalWorkflowExecutor(task)
  }
}