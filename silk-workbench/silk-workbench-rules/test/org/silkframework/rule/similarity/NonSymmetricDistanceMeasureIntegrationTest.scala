package org.silkframework.rule.similarity


import org.silkframework.rule.LinkSpec
import org.silkframework.workspace.SingleProjectWorkspaceProviderTestTrait
import org.silkframework.workspace.activity.linking.EvaluateLinkingActivity
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

/**
  * Tests the reverse property in a DI project.
  */
class NonSymmetricDistanceMeasureIntegrationTest extends AnyFlatSpec with Matchers with SingleProjectWorkspaceProviderTestTrait {
  behavior of "non symmetric distance measures"

  private val linkTaskReverseFalse = "prefixLinking"
  private val linkTaskReverseTrue = "prefixLinkingReverse"

  override def projectPathInClasspath: String = "org/silkframework/rule/similarity/reverseMatching.zip"

  override def workspaceProviderId: String = "inMemoryWorkspaceProvider"

  it should "produce correct results with reverse=false" in {
    linkingResult(linkTaskReverseFalse).links.size mustBe 1
  }

  it should "produce correct results with reverse=true" in {
    linkingResult(linkTaskReverseTrue).links.size mustBe 3
  }

  private def linkingResult(linkTask: String) = {
    val linkExecution = project.task[LinkSpec](linkTask).activity[EvaluateLinkingActivity]
    linkExecution.control.startBlocking()
    linkExecution.control.value()
  }
}
