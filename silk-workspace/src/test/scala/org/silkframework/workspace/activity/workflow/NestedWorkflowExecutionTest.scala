package org.silkframework.workspace.activity.workflow


import org.silkframework.workspace.{ProjectTask, SingleProjectWorkspaceProviderTestTrait}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

abstract class NestedWorkflowExecutionTest extends AnyFlatSpec with Matchers with SingleProjectWorkspaceProviderTestTrait {

  override def projectPathInClasspath: String = "org/silkframework/workspace/activity/workflow/nestedWorkflowsProject.zip"

  override def workspaceProviderId: String = "inMemoryWorkspaceProvider"

  override def projectId: String = "nestedWorkflowsTest"

  // Needs to be overwritten in sub classes
  protected def executeWorkflow(workflow: ProjectTask[Workflow]): Unit

  it should "execute nested workflows" in {
    val proj = project
    val dataset1 = proj.resources.get("persons.csv")
    val dataset2 = proj.resources.get("persons2.csv")
    val dataset3 = proj.resources.get("persons3.csv")
    val overallWorkflow = proj.task[Workflow]("f044b20f-ba34-4035-be2b-9ac33ccdaed3_OverallWorkflow")

    // Make sure that the output datasets are empty
    dataset1.nonEmpty shouldBe true
    dataset2.nonEmpty shouldBe false
    dataset3.nonEmpty shouldBe false

    // Execute the overall workflow, which will execute two nested workflows
    executeWorkflow(overallWorkflow)

    // Make sure that both nested workflows generated output
    dataset1.nonEmpty shouldBe true
    dataset2.nonEmpty shouldBe true
    dataset3.nonEmpty shouldBe true
  }

}
