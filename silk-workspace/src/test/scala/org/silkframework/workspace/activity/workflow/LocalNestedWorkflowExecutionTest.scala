package org.silkframework.workspace.activity.workflow

import org.silkframework.runtime.activity.{ActivityMonitor, Status, UserContext}
import org.silkframework.workspace.ProjectTask

class LocalNestedWorkflowExecutionTest extends NestedWorkflowExecutionTest {

  override protected def executeWorkflow(workflow: ProjectTask[Workflow]): Unit = {
    workflow.activity[LocalWorkflowExecutorGeneratingProvenance].startBlocking()
  }

  it should "not start the nodes of a nested workflow once the enclosing workflow has been cancelled" in {
    val nestedWorkflow = project.task[Workflow]("d9518621-3a4b-4cfa-a464-f06bccd18e6e_Workflow1")
    // A nested workflow runs on the node context of its operator, which the enclosing run switches to Canceling
    val context = new ActivityMonitor[WorkflowExecutionReport]("nested", initialValue = Some(WorkflowExecutionReport(nestedWorkflow)))
    context.status.update(Status.Canceling(None))

    LocalWorkflowExecutor(nestedWorkflow, clearDatasets = false).run(context)(UserContext.Empty)

    context.value().taskReports shouldBe empty // No node has been executed
    context.value().isDone shouldBe true
  }

}
