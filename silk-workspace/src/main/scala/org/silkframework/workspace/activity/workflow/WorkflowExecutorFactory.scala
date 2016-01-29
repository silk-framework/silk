package org.silkframework.workspace.activity.workflow

import org.silkframework.runtime.activity.Activity
import org.silkframework.runtime.plugin.Plugin
import org.silkframework.workspace.Task
import org.silkframework.workspace.activity.TaskActivityFactory

@Plugin(
  id = "executeWorkflow",
  label = "Execute Workflow",
  categories = Array("LinkSpecification"),
  description = "Executes the workflow."
)
case class WorkflowExecutorFactory() extends TaskActivityFactory[Workflow, WorkflowExecutor] {

  override def apply(task: Task[Workflow]): Activity[Unit] = {
    new WorkflowExecutor(task)
  }

}
