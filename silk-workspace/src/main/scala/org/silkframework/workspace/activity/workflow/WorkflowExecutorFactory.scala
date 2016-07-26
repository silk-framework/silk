package org.silkframework.workspace.activity.workflow

import org.silkframework.runtime.activity.Activity
import org.silkframework.runtime.plugin.Plugin
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.activity.TaskActivityFactory

@Plugin(
  id = "ExecuteWorkflow",
  label = "Execute Workflow",
  categories = Array("LinkSpecification"),
  description = "Executes the workflow."
)
case class WorkflowExecutorFactory() extends TaskActivityFactory[Workflow, WorkflowExecutor] {

  override def apply(task: ProjectTask[Workflow]): Activity[WorkflowExecutionReport] = {
    new WorkflowExecutor(task)
  }

}
