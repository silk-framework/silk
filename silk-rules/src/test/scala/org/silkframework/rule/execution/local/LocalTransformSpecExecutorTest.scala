package org.silkframework.rule.execution.local

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.execution.ExecutorRegistry
import org.silkframework.execution.local.LocalExecution
import org.silkframework.rule.TransformSpec

/**
  * Created on 8/23/16.
  */
class LocalTransformSpecExecutorTest extends FlatSpec with MustMatchers with ExecutorRegistry {
  behavior of "Local Transform Specification Executor"

  it should "load from the registry" in {
    executor(TransformSpec(null, null), LocalExecution(false)).getClass mustBe classOf[LocalTransformSpecExecutor]
  }
}