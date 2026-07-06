package org.silkframework.workspace.activity.workflow

import org.silkframework.runtime.activity.Activity
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}
import org.silkframework.runtime.templating.TemplateVariablesParameter
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.activity.TaskActivityFactory

/**
  * A factory that creates a local workflow executor.
  */
@Plugin(
  id = "ExecuteLocalWorkflow",
  label = "Execute locally",
  categories = Array("Workflow"),
  description = "Executes the workflow locally."
)
case class LocalWorkflowExecutorFactory(@Param(label = "Execution variables", value = "Variables for this workflow execution.", visibleInDialog = false)
                                        executionVariables: TemplateVariablesParameter = TemplateVariablesParameter.empty)
  extends TaskActivityFactory[Workflow, LocalWorkflowExecutorGeneratingProvenance] {

  override def apply(task: ProjectTask[Workflow]): Activity[WorkflowExecutionReportWithProvenance] = {
    // Only the overrides are passed here. They are merged with the workflow's execution variables at run start.
    LocalWorkflowExecutorGeneratingProvenance(task, workflowVariables = executionVariables.variables)
  }
}