package org.silkframework.workspace.activity.workflow

import org.silkframework.workspace.{Project, Task}

/**
  * Allows to traverse through the workflow.
  *
  * @param operator The current workflow node
  * @param workflow The workflow task
  */
class WorkflowTraverser(operator: WorkflowNode)(implicit workflow: Task[Workflow]) {

  def task: Task[_] = workflow.project.anyTask(operator.task)

  def inputs: Seq[WorkflowTraverser] = {
    operator.inputs.map(input => new WorkflowTraverser(workflow.data.node(input)))
  }

  def outputs: Seq[WorkflowTraverser] = {
    operator.outputs.map(output => new WorkflowTraverser(workflow.data.node(output)))
  }
}