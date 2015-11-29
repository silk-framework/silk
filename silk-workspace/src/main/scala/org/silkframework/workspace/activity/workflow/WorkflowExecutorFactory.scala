package org.silkframework.workspace.activity.workflow

import org.silkframework.runtime.activity.Activity
import org.silkframework.workspace.Task
import org.silkframework.workspace.activity.TaskActivityFactory

class WorkflowExecutorFactory extends TaskActivityFactory[Workflow, WorkflowExecutor] {

  override def apply(task: Task[Workflow]): Activity[Unit] = {
    new WorkflowExecutor(task)
  }

}
