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

  it should "not collect outer loop variables referenced in nested for-loops" in {
    collect(
      """
        | {% for book in entities %}
        |   {% for chapter in book.chapter %}
        |     {{chapter.name}}
        |   {% endfor %}
        | {% endfor %}
        |""".stripMargin) shouldBe Seq("entities")
  }

  it should "keep variables bound after a for-loop" in {
    collect(
      """
        | {% set x = city %}
        | {% for user in users %}
        |   {{user}}
        | {% endfor %}
        | {{x}}
        |""".stripMargin) shouldBe Seq("city", "users")
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
    collect("{{ x is divisibleby 3 }}") shouldBe Seq("x")
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

  it should "collect the accessed variable for method calls inside complex expressions" in {
    collect("{{ name.trim() ~ 'x' }}") shouldBe Seq("name")
    collect("{{ project.myVar.trim() ~ 'x' }}") shouldBe Seq("project.myVar")
    collect("{{ name.trim().upper() }}") shouldBe Seq("name")
  }

  it should "collect scoped variables inside list and dict literals" in {
    collect("{% set x = [project.myVar] %}") shouldBe Seq("project.myVar")
    collect("{% set d = {\"key\": project.myVar} %}") shouldBe Seq("project.myVar")
  }

  it should "not collect variable-like content of string literals" in {
    collect("{{ 'literal.with(' ~ name }}") shouldBe Seq("name")
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

  it should "collect references in range brackets and tolerate omitted bounds" in {
    collect("{{ items[:3] }}") shouldBe Seq("items")
    collect("{{ items[1:] }}") shouldBe Seq("items")
    collect("{{ items[1:maxIndex] }}") shouldBe Seq("items", "maxIndex")
    collect("{{ items[1:project.maxIndex] }}") shouldBe Seq("items", "project.maxIndex")
  }

  it should "not keep macro parameters bound after the definition" in {
    collect("{% macro greet(name) %}Hi {{name}}{% endmacro %}{{name}}") shouldBe Seq("name")
  }

  it should "not collect named argument names at call sites" in {
    collect("{{ items | join(attribute='x') }}") shouldBe Seq("items")
  }

  it should "collect free variables of macro default values" in {
    collect("{% macro f(x=defaultCity) %}{{x}}{% endmacro %}") shouldBe Seq("defaultCity")
  }

  it should "collect variables that are referenced before their binding" in {
    collect("{{x}} {% set x = 1 %}") shouldBe Seq("x")
    collect("{% set x = x | default('d') %}") shouldBe Seq("x")
  }

  it should "bind the target of a block set and collect its body" in {
    collect("{% set g %}Hello {{user}}{% endset %}{{g}}") shouldBe Seq("user")
  }

  it should "keep variables that are set inside an if block bound after it" in {
    collect("{% if c %}{% set g = 'a' %}{% else %}{% set g = 'b' %}{% endif %}{{g}}") shouldBe Seq("c")
  }

  it should "collect call tag arguments" in {
    collect("{% call renderIt(myTitle) %}text{% endcall %}") shouldBe Seq("myTitle")
  }

  it should "split the for expression at the first 'in' only" in {
    collect("{% for x in data.get(' in ') %}{{x}}{% endfor %}") shouldBe Seq("data")
  }

  it should "filter loop references at any depth but keep scoped variables named loop" in {
    collect("{% for x in items %}{{loop.previtem.name}}{% endfor %}") shouldBe Seq("items")
    collect("{% for x in items %}{{project.loop}}{% endfor %}") shouldBe Seq("items", "project.loop")
  }

  it should "treat identifier dict keys as literal names" in {
    collect("{% set d = {key: value} %}") shouldBe Seq("value")
  }

  it should "not collect expressions inside raw blocks" in {
    collect("{% raw %}{{foo}}{% endraw %}") shouldBe Seq()
  }

  it should "collect variables in tuples" in {
    collect("{% set t = (a, b) %}") shouldBe Seq("a", "b")
  }

  private def collect(template: String): Seq[String] = {
    val node = JinjaTemplateEngine().compile(template).node
    new JinjaVariableCollector().collect(node).unboundVars.map(_.scopedName)
  }

}
