package org.silkframework.plugins.templating.jinja

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class JinjaVariableCollectorTest extends AnyFlatSpec with Matchers {

  behavior of "JinjaVariableCollector"

  it should "collect plain variable replacements" in {
    collect("This is {{name}} from {{city}}.") shouldBe Seq("name", "city")
  }

  it should "collect variables in conditions" in {
    collect(
      """
        | {% if title == "Mayor" %}
        |   This is the Mayor.
        | {% else %}
        |   This is {{name}}.
        | {% endif %}
        |""".stripMargin) shouldBe Seq("title", "name")

    collect(
      """
        | {% if var1 == "1" %}
        |   Case 1
        | {% elif var2 == "2"  %}
        |   Case 2
        |   {% if var3 == "3" %}
        |     Case 3
        |   {% elif var4 == "4"  %}
        |     Case 4
        |   {% endif %}
        | {% endif %}
        |""".stripMargin) shouldBe Seq("var1", "var2", "var3", "var4")
  }

  it should "collect variables inside expressions" in {
    val variables = collect("{{names | removeDuplicates | concatMultiValues(', ')}}")
    variables shouldBe Seq("names")
  }

  it should "collect variables inside do statements" in {
    val variables = collect("{% do name %}")
    variables shouldBe Seq("name")
  }

  it should "collect variables in for-loops" in {
    collect(
      """
        | {% for user in users %}
        |   This is {{user}} at index {{loop.index}}.
        | {% endfor %}
        |""".stripMargin) shouldBe Seq("users")

    collect(
      """
        | {% for user in users %}
        |   {{ inputs | getValueByIndex(loop.index) }}
        | {% endfor %}
        |""".stripMargin) shouldBe Seq("users", "inputs")
  }

  it should "collect variables in set expressions" in {
    collect(
      """
        | {% set location, country, timestamp = city, 'Germany', time %}
        | {{user}} is from {{location}}, {{country}} ({{timestamp}})
        |
        |""".stripMargin) shouldBe Seq("city", "time", "user")
    collect(
      """
        | {% for user in users %}
        |   {% set location, country, timestamp = city, 'Germany', time %}
        |   {{user}} is from {{location}}, {{country}} ({{timestamp}})
        | {% endfor %}
        |
        |""".stripMargin) shouldBe Seq("users", "city", "time")
  }

  it should "collect variables in tests" in {
    collect(
      """
        | {% if title is defined %}
        |   Is defined
        | {% endif %}
        |""".stripMargin) shouldBe Seq("title")
  }

  it should "collect scoped variables in simple expressions" in {
    collect("This is {{project.name}} from {{global.city}}.") shouldBe Seq("project.name", "global.city")
  }

  it should "collect scoped variables in complex expressions" in {
    collect("{{ input.parameters.graph ~ \"/data\" }}") shouldBe Seq("input.parameters.graph")
    collect("{{ a.b ~ c.d }}") shouldBe Seq("a.b", "c.d")
  }

  it should "collect variables used in method calls" in {
    collect(
      """
        | {{ row.uri("subject") }}
        | {% if row.exists("somePath") %}
        |   Plain: {{ row.plainLiteral("somePath") }}
        |   Raw: {{ row.rawUnsafe("trustedValuePath") }}
        | {% endif %}
        |""".stripMargin) shouldBe Seq("row")
  }

  it should "don't fail on empty expressions" in {
    collect("{{ }}".stripMargin) shouldBe Seq.empty
  }

  it should "don't collect bound variables in macros" in {
    collect(
      """ {% macro foo(name) %}
        |   Hello {{name}}
        | {% endmacro %}
        | {{ foo('John') }}""".stripMargin) shouldBe Seq()
  }

  it should "collect unbound variables in macros" in {
    collect(
      """ {% macro foo(street, number) %}
        |   {{street}} {{number}}, {{country}}
        | {% endmacro %}
        | {{ foo('Hainstraße', '8') }}""".stripMargin) shouldBe Seq("country")
  }

  private def collect(template: String): Seq[String] = {
    val node = JinjaTemplateEngine().compile(template).node
    new JinjaVariableCollector().collect(node).unboundVars.map(_.scopedName)
  }

}
