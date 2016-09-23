package org.silkframework.rule.execution.local

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.execution.ExecutorRegistry
import org.silkframework.execution.local.LocalExecution
import org.silkframework.rule.LinkSpec

/**
  * Created on 8/23/16.
  */
class LocalLinkSpecificationExecutorTest extends FlatSpec with MustMatchers with ExecutorRegistry {
  behavior of "Local Link Specification Executor"

  it should "load from the registry" in {
    executor(LinkSpec(), LocalExecution(false)).getClass mustBe classOf[LocalLinkSpecificationExecutor]
  }
}
