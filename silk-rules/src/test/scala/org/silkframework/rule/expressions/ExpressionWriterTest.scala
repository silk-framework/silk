package org.silkframework.rule.expressions

import org.silkframework.rule.input.Input

class ExpressionWriterTest extends ExpressionTestBase {

  behavior of "ExpressionWriter"

  override protected def check(expr: String, result: Input) = {
    ExpressionWriter(result) shouldBe expr
  }

}
