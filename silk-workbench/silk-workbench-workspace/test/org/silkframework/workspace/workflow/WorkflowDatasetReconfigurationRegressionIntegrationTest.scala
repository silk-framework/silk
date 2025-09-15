package org.silkframework.workspace.workflow

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.workspace.SingleProjectWorkspaceProviderTestTrait

class WorkflowDatasetReconfigurationRegressionIntegrationTest extends AnyFlatSpec with Matchers with SingleProjectWorkspaceProviderTestTrait {
  behavior of "Workflow re-configuration"

  override def projectPathInClasspath: String = "diProjects/project-CMEM-5058.zip"

  /*
    The workflow re-configures the input XML dataset with a different file parameter. After execution, the original parameter is deleted.
   */
  private val workflowId = "test-workflow"
  private val originalInputFile = "persons.xml"

  override def workspaceProviderId: String = "inMemoryWorkspaceProvider"

  it should "not delete dataset file when re-configuring the file parameter value in a workflow" in {
    val originalInputResourceSize = project.resources.get(originalInputFile).size.get
    originalInputResourceSize must be > 1L
    executeWorkflow(workflowId)
    val fileAfter = project.resources.get(originalInputFile)
    fileAfter.exists mustBe true
    fileAfter.size.get mustBe originalInputResourceSize
  }
}
