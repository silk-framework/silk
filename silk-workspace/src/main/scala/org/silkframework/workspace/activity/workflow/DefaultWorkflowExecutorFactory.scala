package org.silkframework.workspace.activity.workflow

import org.silkframework.runtime.activity.Activity
import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.activity.TaskActivityFactory

@Plugin(
  id = "ExecuteDefaultWorkflow",
  label = "Default execution",
  categories = Array("WorkflowExecution"),
  description = "Executes a workflow with the local executor"
)
case class DefaultWorkflowExecutorFactory() extends TaskActivityFactory[Workflow, LocalWorkflowExecutorGeneratingProvenance] {

  override def apply(task: ProjectTask[Workflow]): Activity[WorkflowExecutionReportWithProvenance] = {
    LocalWorkflowExecutorGeneratingProvenance(task)
  }
}
