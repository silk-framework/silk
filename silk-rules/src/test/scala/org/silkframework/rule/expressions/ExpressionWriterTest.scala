package org.silkframework.rule.expressions

import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.config.Prefixes
import org.silkframework.rule.plugins.transformer.normalize.LowerCaseTransformer
import org.silkframework.rule.plugins.transformer.replace.ReplaceTransformer

class ExpressionWriterTest extends FlatSpec with Matchers {

  val generator = new ExpressionGenerator
  import generator._

  behavior of "ExpressionWriter"

  implicit val prefixes = Prefixes(Map("f" -> "http://example.org/prefix"))

  it should "generate expression for paths" in {
    ExpressionWriter(path("f:somePath")) shouldBe
      "f:somePath"
  }

  it should "generate expressions for constants" in {
    ExpressionWriter(constant("3.14")) shouldBe
      "3.14"
  }

  it should "generate expressions for function invocations" in {
    ExpressionWriter(func(LowerCaseTransformer(), path("f:somePath"))) shouldBe
      "lowerCase(f:somePath)"
  }

  it should "generate expressions for function invocations with parameters" in {
    ExpressionWriter(func(ReplaceTransformer(search = "x", replace = "y"), Seq(path("f:path1"), path("f:path2")))) shouldBe
      "replace[search:x;replace:y](f:path1;f:path2)"
  }

  it should "escape characters in function parameter values" in {
    ExpressionWriter(func(ReplaceTransformer(search = "xxx;yyy", replace = "yyy]xxx"), Seq(path("f:path1"), path("f:path2")))) shouldBe
      "replace[search:xxx\\;yyy;replace:yyy\\]xxx](f:path1;f:path2)"
  }

}
