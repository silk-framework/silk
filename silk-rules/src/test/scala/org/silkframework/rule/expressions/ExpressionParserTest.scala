package org.silkframework.rule.expressions

import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.config.Prefixes
import org.silkframework.rule.plugins.transformer.numeric.{AggregateNumbersTransformer, LogarithmTransformer}
import org.silkframework.rule.input.{Input, PathInput, TransformInput}
import org.silkframework.rule.plugins.transformer.replace.ReplaceTransformer

class ExpressionParserTest extends FlatSpec with Matchers {

  val generator = new ExpressionGenerator
  import generator._

  behavior of "ExpressionParser"

  implicit val prefixes = Prefixes(Map("f" -> "http://example.org/prefix"))

  it should "parse constants" in {
    check(
      expr = "3.14",
      result = constant("3.14")
    )
  }

  it should "parse variables" in {
    check(
      expr = "x",
      result = path("x")
    )
    check(
      expr = "f:weight",
      result = path("f:weight")
    )
  }

  it should "parse multiplications between two variables" in {
    check(
      expr = "f:height * f:weight",
      result = numOp(path("f:height"), "*", path("f:weight"))
    )
  }

  it should "parse multiplications between three variables" in {
    check(
      expr = "a * b * c",
      result = numOp(numOp(path("a"), "*", path("b")), "*", path("c"))
    )
  }

  it should "parse additions between three variables" in {
    check(
      expr = "a + b + c",
      result = numOp(numOp(path("a"), "+", path("b")), "+", path("c"))
    )
    check(
      expr = "a + b - c",
      result = numOp(numOp(path("a"), "+", path("b")), "-", path("c"))
    )
  }

  it should "parse multiplications before additions" in {
    check(
      expr = "a + b * c",
      result = numOp(path("a"), "+", numOp(path("b"), "*", path("c")))
    )
    check(
      expr = "a - b / c",
      result = numOp(path("a"), "-", numOp(path("b"), "/", path("c")))
    )
  }

  it should "parse multiplications with a constant" in {
    check(
      expr = "a * 1.0",
      result = numOp(path("a"), "*", constant("1.0"))
    )
  }

  it should "parse expressions with brackets" in {
    check(
      expr = "a * (b + 1.0)",
      result = numOp(path("a"), "*", numOp(path("b"), "+", constant("1.0")))
    )
  }

  it should "parse function invocations" in {
    check(
      expr = "log(x)",
      result = func(LogarithmTransformer(), path("x"))
    )
    check(
      expr = "log(1.0)",
      result = func(LogarithmTransformer(), constant("1.0"))
    )
  }

  it should "parse function invocations with expressions inside" in {
    check(
      expr = "log(a * b)",
      result = func(LogarithmTransformer(), numOp(path("a"), "*", path("b")))
    )
  }

  it should "parse expressions with function invocations" in {
    check(
      expr = "3.0 + log(x) * y",
      result = numOp(constant("3.0"), "+", numOp(func(LogarithmTransformer(), path("x")), "*", path("y")))
    )
  }

  it should "parse function invocations with a single parameter" in {
    check(
      expr = "log[base:16](x)",
      result = func(LogarithmTransformer(base = 16), path("x"))
    )
  }

  it should "parse function invocations with multiple parameters" in {
    check(
      expr = "replace[search:x;replace:y](x)",
      result = func(ReplaceTransformer(search = "x", replace = "y"), path("x"))
    )
  }

  it should "parse function invocations with escaped parameter values" in {
    check(
      expr = "replace[search:yyy\\:xxx;replace:xxx\\]yyy](x)",
      result = func(ReplaceTransformer(search = "yyy:xxx", replace = "xxx]yyy"), path("x"))
    )
  }

  it should "parse complex function invocations" in {
    check(
      expr = "aggregateNumbers[operator:max](5;3)",
      result = func(AggregateNumbersTransformer(operator = "max"), Seq(constant("5"), constant("3")))
    )
  }

  private def check(expr: String, result: Input) = {
    normalizeIds(ExpressionParser.parse(expr)) should be (normalizeIds(result))
  }

  private def normalizeIds(input: Input): Input = input match {
    case PathInput(id, path) => PathInput("id", path)
    case TransformInput(id, transformer, inputs) => TransformInput("id", transformer, inputs.map(normalizeIds))
  }

}
