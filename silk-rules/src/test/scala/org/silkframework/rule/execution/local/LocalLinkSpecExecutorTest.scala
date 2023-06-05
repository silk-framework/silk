package org.silkframework.rule.execution.local

import org.silkframework.execution.ExecutorRegistry
import org.silkframework.execution.local.LocalExecution
import org.silkframework.rule.LinkSpec
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class LocalLinkSpecExecutorTest extends AnyFlatSpec with Matchers with ExecutorRegistry {
  behavior of "Local Link Specification Executor"

  it should "load from the registry" in {
    executor(LinkSpec(), LocalExecution(false)).getClass mustBe classOf[LocalLinkSpecExecutor]
  }

  it should "adapt link limit correctly" in {
    LinkSpec.adaptLinkLimit(LinkSpec.MAX_LINK_LIMIT - 1) mustBe LinkSpec.MAX_LINK_LIMIT - 1
    LinkSpec.adaptLinkLimit(LinkSpec.MAX_LINK_LIMIT + 1) mustBe LinkSpec.MAX_LINK_LIMIT
  }
}
