package org.silkframework.rule.expressions

import org.silkframework.rule.input.{Input, PathInput, TransformInput}

class ExpressionParserTest extends ExpressionTestBase {

  import generator._

  behavior of "ExpressionParser"

  // Currently not supported by the expression writer
  it should "parse expressions with brackets" in {
    check(
      expr = "a * (b + 1.0)",
      tree = numOp(path("a"), "*", numOp(path("b"), "+", constant("1.0")))
    )
  }

  override protected def check(expr: String, tree: Input) = {
    normalizeIds(ExpressionParser.parse(expr)) should be (normalizeIds(tree))
  }

  private def normalizeIds(input: Input): Input = input match {
    case PathInput(id, path) => PathInput("id", path)
    case TransformInput(id, transformer, inputs) => TransformInput("id", transformer, inputs.map(normalizeIds))
  }

}
