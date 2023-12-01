package controllers.workspace

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.runtime.plugin.ParameterValues
import org.silkframework.workspace.SingleProjectWorkspaceProviderTestTrait
import org.silkframework.workspace.activity.workflow.{LocalWorkflowExecutorGeneratingProvenance, Workflow}

class WorkflowExecutionIntegrationTest extends AnyFlatSpec with Matchers with SingleProjectWorkspaceProviderTestTrait {
    override def projectPathInClasspath: String = "diProjects/workflow-execution-integration-test-project.zip"

  override def workspaceProviderId: String = "inMemory"

  private val workflow = "workflow"

  it should "execute a workflow that has flexible-noPort connections" in {
    val workflowExecutionActivity = project.task[Workflow](workflow).activity[LocalWorkflowExecutorGeneratingProvenance]
    workflowExecutionActivity.startBlocking(ParameterValues.empty)
  }
}
