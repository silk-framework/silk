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
case class LocalWorkflowExecutorFactory(@Param(label = "Workflow variables", value = "Variables for this workflow execution.", visibleInDialog = false)
                                        workflowVariables: TemplateVariablesParameter = TemplateVariablesParameter.empty)
  extends TaskActivityFactory[Workflow, LocalWorkflowExecutorGeneratingProvenance] {

  override def apply(task: ProjectTask[Workflow]): Activity[WorkflowExecutionReportWithProvenance] = {
    val mergedVars = task.variables.merge(workflowVariables.variables)
    LocalWorkflowExecutorGeneratingProvenance(task, workflowVariables = mergedVars)
  }
}