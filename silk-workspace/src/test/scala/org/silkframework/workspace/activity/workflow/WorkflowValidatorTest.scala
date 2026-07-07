package org.silkframework.workspace.activity.workflow

import org.silkframework.config.{MetaData, PlainTask, Task}
import org.silkframework.runtime.activity.TestUserContextTrait
import org.silkframework.runtime.plugin.types.IntOptionParameter
import org.silkframework.runtime.resource.InMemoryResourceManager
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

  it should "only allow empty or positive workflow parallel execution limits" in {
    noException should be thrownBy Workflow(maxParallelExecutions = IntOptionParameter(None))
    noException should be thrownBy Workflow(maxParallelExecutions = IntOptionParameter(Some(1)))
    noException should be thrownBy Workflow(maxParallelExecutions = IntOptionParameter(Some(3)))

    an[IllegalArgumentException] should be thrownBy Workflow(maxParallelExecutions = IntOptionParameter(Some(0)))
    an[IllegalArgumentException] should be thrownBy Workflow(maxParallelExecutions = IntOptionParameter(Some(-1)))
  }

  it should "invalidate serialized workflows that bypass nested workflow validation during project load" in {
    val projectId = "WorkflowLoadValidationTest"
    val projectConfig = org.silkframework.workspace.ProjectConfig(projectId, metaData = MetaData(Some(projectId)))
    val resources = InMemoryResourceManager()

    workspaceProvider.putProject(projectConfig)
    workspaceProvider.putTask(projectId, createWorkflow("workflow1", nestedWorkflowIds = Seq.empty), resources)
    workspaceProvider.putTask(projectId, createWorkflow("workflow2", nestedWorkflowIds = Seq("workflow1")), resources)
    workspaceProvider.putTask(projectId, createWorkflow("workflow3", nestedWorkflowIds = Seq("workflow2")), resources)

    val project = new Project(projectConfig, workspaceProvider, resources, userContext)

    project.taskOption[Workflow]("workflow1") should not be empty
    project.taskOption[Workflow]("workflow2") shouldBe empty
    project.taskOption[Workflow]("workflow3") shouldBe empty
    project.loadingErrors.map(_.taskId) should contain allOf("workflow2", "workflow3")
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
