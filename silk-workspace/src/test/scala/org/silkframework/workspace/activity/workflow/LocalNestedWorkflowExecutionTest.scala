package org.silkframework.workspace.activity.workflow

import org.silkframework.runtime.activity.{ActivityMonitor, Status, UserContext}
import org.silkframework.workspace.ProjectTask

class LocalNestedWorkflowExecutionTest extends NestedWorkflowExecutionTest {

  override protected def executeWorkflow(workflow: ProjectTask[Workflow]): Unit = {
    workflow.activity[LocalWorkflowExecutorGeneratingProvenance].startBlocking()
  }

  it should "not start the nodes of a nested workflow once the enclosing workflow has been cancelled" in {
    val nestedWorkflow = project.task[Workflow]("d9518621-3a4b-4cfa-a464-f06bccd18e6e_Workflow1")
    // A nested workflow runs on the node context of its operator. The enclosing run switches that context to Canceling
    // once it is running; a cancel that arrives before that is only visible on the enclosing run's own context.
    val enclosingContext = new ActivityMonitor[Unit]("enclosing")
    enclosingContext.status.update(Status.Canceling(None))
    val context = new ActivityMonitor[WorkflowExecutionReport]("nested", parent = Some(enclosingContext),
      initialValue = Some(WorkflowExecutionReport(nestedWorkflow)))

    val error = intercept[WorkflowExecutionException] {
      LocalWorkflowExecutor(nestedWorkflow, clearDatasets = false).run(context)(UserContext.Empty)
    }

    // The nested run is incomplete and fails, so that the enclosing report marks its node as failed
    error.getMessage shouldBe "The workflow has been cancelled. Executed 0 of 3 nodes."
    context.value().taskReports shouldBe empty // No node has been executed
    context.value().error shouldBe Some(error.getMessage)
    context.value().isDone shouldBe true
  }

}
