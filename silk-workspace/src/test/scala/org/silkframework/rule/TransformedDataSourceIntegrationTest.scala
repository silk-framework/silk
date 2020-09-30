package org.silkframework.rule

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.workspace.SingleProjectWorkspaceProviderTestTrait
import org.silkframework.workspace.activity.linking.EvaluateLinkingActivity

class TransformedDataSourceIntegrationTest extends FlatSpec with MustMatchers with SingleProjectWorkspaceProviderTestTrait {
  behavior of "Transformed DataSource used as input for a linking task evaluation"

  override def projectPathInClasspath: String = "diProjects/brokenLinkEvaluationWithTransformationAsInputSource.zip"

  override def workspaceProviderId: String = "inMemory"

  private val linkingTaskId = "linkSourceAndTarget"

  it should "generate the expected links" in {
    val linkingTask = project.task[LinkSpec](linkingTaskId)
    val control = linkingTask.activity[EvaluateLinkingActivity].control
    control.startBlocking()
    control.value.get.get.links.size mustBe 100
  }
}
