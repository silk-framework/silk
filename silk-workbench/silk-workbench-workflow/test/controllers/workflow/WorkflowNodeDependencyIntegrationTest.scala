package controllers.workflow

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.workspace.SingleProjectWorkspaceProviderTestTrait

import scala.util.{Success, Try}

class WorkflowNodeDependencyIntegrationTest extends AnyFlatSpec with Matchers with SingleProjectWorkspaceProviderTestTrait {

  override def projectPathInClasspath: String = "workflow-dependency-test-project.zip"

  override def projectId: String = "nestedWorkflowsTest"

  override def workspaceProviderId: String = "inMemory"

  it should "execute a workflow that connects independent parts only via dependency connections" in {
    noException mustBe thrownBy {
      executeWorkflow("dependency-workflow")
    }
  }
}