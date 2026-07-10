package org.silkframework.rule

import org.silkframework.config.PlainTask
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Identifier
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.runtime.plugin.TaskResolver

class TaskResolverTest extends AnyFlatSpec with Matchers {

  behavior of "TaskResolver"

  it should "resolve typed tasks" in {
    val expected = PlainTask[RuleBlockSpec](Identifier("ruleBlock"), RuleBlockSpec.empty)
    val resolver = new TaskResolver {
      override def resolveTyped[T <: org.silkframework.config.TaskSpec : scala.reflect.ClassTag](id: Identifier) =
        expected.asInstanceOf[PlainTask[T]]
    }

    resolver.resolveTyped[RuleBlockSpec](Identifier("ruleBlock")) shouldBe expected
  }

  it should "fail explicitly if no resolver is available" in {
    the[ValidationException] thrownBy {
      TaskResolver.empty.resolveTyped[RuleBlockSpec](Identifier("missing"))
    }
  }
}
