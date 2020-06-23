package org.silkframework.rule.similarity

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.rule.LinkSpec
import org.silkframework.workspace.SingleProjectWorkspaceProviderTestTrait
import org.silkframework.workspace.activity.linking.EvaluateLinkingActivity

/**
  * Tests the reverse property in a DI project.
  */
class NonSymmetricDistanceMeasureIntegrationTest extends FlatSpec with MustMatchers with SingleProjectWorkspaceProviderTestTrait {
  behavior of "non symmetric distance measures"

  private val linkTaskReverseFalse = "prefixLinking"
  private val linkTaskReverseTrue = "prefixLinkingReverse"

  override def projectPathInClasspath: String = "org/silkframework/rule/similarity/reverseMatching.zip"

  override def workspaceProviderId: String = "inMemory"

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
