package org.silkframework.plugins.templating.jinja

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.runtime.templating.TemplateMethodUsage

class JinjaMethodCollectorTest extends AnyFlatSpec with Matchers {

  behavior of "JinjaMethodCollector"

  it should "collect a method call in an expression node" in {
    collect("""{{ row.uri("subject") }}""", "row") shouldBe Seq(TemplateMethodUsage("uri", "subject"))
  }

  it should "collect a method call in an if tag helper" in {
    collect("""{% if row.exists("somePath") %}yes{% endif %}""", "row") shouldBe Seq(TemplateMethodUsage("exists", "somePath"))
  }

  it should "collect multiple method calls across nodes" in {
    collect(
      """
        |{{ row.uri("subject") }}
        |{% if row.exists("somePath") %}
        |  {{ row.plainLiteral("somePath") }}
        |  {{ row.rawUnsafe("trustedValuePath") }}
        |{% endif %}
        |""".stripMargin, "row"
    ) shouldBe Seq(
      TemplateMethodUsage("uri", "subject"),
      TemplateMethodUsage("exists", "somePath"),
      TemplateMethodUsage("plainLiteral", "somePath"),
      TemplateMethodUsage("rawUnsafe", "trustedValuePath")
    )
  }

  it should "only collect methods on the requested variable" in {
    collect(
      """{{ row.uri("subject") }} {{ other.uri("subject") }}""",
      "row"
    ) shouldBe Seq(TemplateMethodUsage("uri", "subject"))
  }

  it should "collect method calls using single-quoted parameters" in {
    collect("""{{ row.uri('subject') }}""", "row") shouldBe Seq(TemplateMethodUsage("uri", "subject"))
  }

  it should "return an empty sequence when no methods are called on the variable" in {
    collect("""INSERT DATA { <urn:a:b> <urn:c:d> "hello" }""", "row") shouldBe Seq.empty
  }

  it should "return an empty sequence when the variable is not present" in {
    collect("""{{ other.uri("subject") }}""", "row") shouldBe Seq.empty
  }

  it should "not collect method calls without a string constant parameter" in {
    // row.method(var) — non-constant parameter, should not be collected
    collect("""{{ row.uri(subject) }}""", "row") shouldBe Seq.empty
  }

  private def collect(template: String, variableName: String): Seq[TemplateMethodUsage] = {
    val node = JinjaTemplateEngine().compile(template).node
    new JinjaMethodCollector().collect(node, variableName)
  }
}