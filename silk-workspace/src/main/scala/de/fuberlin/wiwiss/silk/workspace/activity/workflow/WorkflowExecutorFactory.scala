package de.fuberlin.wiwiss.silk.workspace.activity.workflow

import de.fuberlin.wiwiss.silk.runtime.activity.Activity
import de.fuberlin.wiwiss.silk.workspace.activity.TaskActivityFactory
import de.fuberlin.wiwiss.silk.workspace.{Task}

class WorkflowExecutorFactory extends TaskActivityFactory[Workflow, WorkflowExecutor, Unit] {

  override def apply(task: Task[Workflow]): Activity[Unit] = {
    new WorkflowExecutor(task)
  }

}
