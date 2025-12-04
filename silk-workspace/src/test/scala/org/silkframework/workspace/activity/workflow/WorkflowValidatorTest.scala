package org.silkframework.workspace.activity.workflow


import org.silkframework.config.{PlainTask, Task}
import org.silkframework.runtime.activity.TestUserContextTrait
import org.silkframework.util.Identifier
import org.silkframework.workspace.exceptions.TaskValidationException
import org.silkframework.workspace.{Project, TestWorkspaceProviderTestTrait}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class WorkflowValidatorTest extends AnyFlatSpec with Matchers with TestWorkspaceProviderTestTrait with TestUserContextTrait  {

  behavior of "WorkflowValidator"

  it should "not allow creating workflows that contain workflows that already contain nested workflows" in {
    val project = retrieveOrCreateProject("WorkflowNestingTest")

    val workflow1 = createWorkflow("workflow1", nestedWorkflowIds = Seq.empty)
    val workflow2 = createWorkflow("workflow2", nestedWorkflowIds = Seq("workflow1"))
    val workflow3 = createWorkflow("workflow3", nestedWorkflowIds = Seq("workflow2"))

    noException should be thrownBy update(project, workflow1)
    noException should be thrownBy update(project, workflow2)
    an[TaskValidationException] should be thrownBy update(project, workflow3)
  }

  it should "not allow creating nested workflows that are already referenced by existing workflows" in {
    val project = retrieveOrCreateProject("WorkflowNestingTest")

    val workflow1 = createWorkflow("workflow1", nestedWorkflowIds = Seq.empty)
    val workflow2 = createWorkflow("workflow2", nestedWorkflowIds = Seq.empty)
    val workflow3 = createWorkflow("workflow3", nestedWorkflowIds = Seq("workflow2"))

    noException should be thrownBy update(project, workflow1)
    noException should be thrownBy update(project, workflow2)
    noException should be thrownBy update(project, workflow3)

    val workflow2Updated = createWorkflow("workflow2", nestedWorkflowIds = Seq("workflow1"))
    an[TaskValidationException] should be thrownBy update(project, workflow2Updated)
  }

  private def update(project: Project, workflow: Task[Workflow]): Unit = {
    project.updateTask(workflow.id, workflow.data)
  }

  private def createWorkflow(id: Identifier, nestedWorkflowIds: Seq[Identifier]): Task[Workflow] = {
    val nestedWorkflows =
      for(nestedWorkflow <- nestedWorkflowIds) yield {
        WorkflowOperator(
          inputs = Seq.empty,
          task = nestedWorkflow,
          outputs = Seq.empty,
          errorOutputs = Seq.empty,
          position = (0, 0),
          nodeId = Identifier.random,
          outputPriority = None,
          configInputs = Seq.empty,
          dependencyInputs = Seq.empty
        )
      }

    PlainTask(id, Workflow(operators = WorkflowOperatorsParameter(nestedWorkflows)))
  }

  override def workspaceProviderId: String = "inMemoryWorkspaceProvider"

}
