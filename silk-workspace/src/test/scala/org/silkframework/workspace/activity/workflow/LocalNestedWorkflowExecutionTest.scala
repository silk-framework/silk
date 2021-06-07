package org.silkframework.workspace.activity.workflow

import org.silkframework.workspace.ProjectTask

class LocalNestedWorkflowExecutionTest extends NestedWorkflowExecutionTest {

  override protected def executeWorkflow(workflow: ProjectTask[Workflow]): Unit = {
    workflow.activity[LocalWorkflowExecutorGeneratingProvenance].startBlocking()
  }

}
