package org.silkframework.rule.expressions

import org.silkframework.rule.input.Input

class ExpressionWriterTest extends ExpressionTestBase {

  behavior of "ExpressionWriter"

  override protected def check(expr: String, tree: Input) = {
    ExpressionWriter(tree) shouldBe expr
  }

}
