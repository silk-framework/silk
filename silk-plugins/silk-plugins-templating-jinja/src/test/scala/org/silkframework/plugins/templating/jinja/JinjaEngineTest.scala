package org.silkframework.plugins.templating.jinja

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.runtime.templating.{TemplateVariableValue, VariableScope}
import org.silkframework.runtime.templating.exceptions.UnboundVariablesException

import java.io.{StringWriter, Writer}
import scala.collection.immutable.ArraySeq
import scala.jdk.CollectionConverters.{MapHasAsJava, SeqHasAsJava}

class JinjaEngineTest extends AnyFlatSpec with Matchers {

  behavior of "JinjaEngine"

  it should "fail if variables are not bound" in {
    intercept[UnboundVariablesException](
      evaluate(
        template = "{{name}} {{location}}",
        values = Map("firstName"-> Seq("John"))
      )
    ).missingVars.map(_.name) shouldBe Seq("name", "location")

    intercept[UnboundVariablesException](
      evaluate(
        template = "{{name | lower}}",
        values = Map("firstName"-> Seq("John"))
      )
    ).missingVars.map(_.name) shouldBe Seq("name")
  }

  it should "let flat values take precedence over variable scopes of the same name" in {
    val writer = new StringWriter()
    val compiledTemplate = JinjaTemplateEngine().compile("{{project}}/{{execution.myVar}}")
    compiledTemplate.evaluate(Seq(
      new TemplateVariableValue("project", VariableScope.empty, Seq("flatValue")),
      new TemplateVariableValue("title", VariableScope("project"), Seq("scopedValue")),
      new TemplateVariableValue("myVar", VariableScope("execution"), Seq("execValue"))
    ), writer)
    writer.toString shouldBe "flatValue/execValue"
  }

  it should "support transformer plugins to be used as filters" in {
    evaluate(
      template = "{{name | lowerCase}}",
      values = Map("name"-> Seq("John"))
    ) shouldBe "john"

    evaluate(
      template = "{{names | removeDuplicates | concatMultiValues(', ')}}",
      values = Map("names"-> Seq("John", "Max", "John"))
    ) shouldBe "John, Max"
  }

  it should "support combining built-in filters with DataIntegration transformer filters" in {
    evaluate(
      template = "{{input | lower | tokenize(',') | join('-')}}",
      values = Map("input"-> Seq("A,B,C"))
    ) shouldBe "a-b-c"
  }

  it should "support complex templates" in {
    val template =
      """
        | {% for user in users %}
        |   {% set location, country = city, 'Germany' %}
        |   {{user}} is from {{location}}, {{country}}
        | {% endfor %}
        |
        |""".stripMargin

    val values = Map(
      "users"-> Seq("John", "Max"),
      "city" -> Seq("Berlin")
    )

    val expectedLines = Seq(
      "John is from Berlin, Germany",
      "Max is from Berlin, Germany"
    )

    lines(evaluate(template, values)) shouldBe expectedLines
  }

  it should "support templates with macros" in {
    val template =
      """ {% macro foo() %}
        |    {{ "hello world" }}
        | {% endmacro %}
        | {{ foo() }}""".stripMargin

    lines(evaluate(template, Map.empty)) shouldBe Seq("hello world")
  }

  it should "support templates with macros with parameters" in {
    val template =
      """ {% macro foo(name) %}
        |    Hello {{name}}
        | {% endmacro %}
        | {{ foo('John') }}""".stripMargin

    lines(evaluate(template, Map.empty)) shouldBe Seq("Hello John")
  }

  it should "support loop cycle helper" in {
    val template =
      """{% for nr in nrs %}
        |    {{ loop.cycle('odd', 'even') }} {{nr}}
        |{% endfor %}""".stripMargin

    lines(evaluate(template, Map("nrs" -> Seq("1", "2")))) shouldBe Seq("odd 1", "even 2")
  }

  it should "support call and caller()" in {
    val template = """{% macro renderIt(title, class='default') -%}
                     |    {{title}} ({{class}}) [{{caller()}}]
                     |{%- endmacro %}
                     |
                     |{% call renderIt('Titel') %}
                     |caller text
                     |{% endcall %}""".stripMargin

    lines(evaluate(template, Map.empty)) shouldBe Seq("Titel (default) [", "caller text", "]")
  }

  // FIXME: jinjava does not support filter tags yet
  it should "support filter blocks" ignore {
    val template = """{% filter upper %}
                     |    to upper
                     |{% endfilter %}""".stripMargin
    evaluate(template, Map.empty).trim shouldBe "TO UPPER"
  }

  it should "set and use variables" in {
    val template =
      """{% set newVar = "new var" %}
        |A {{newVar}}
        |""".stripMargin
    evaluate(template, Map.empty).trim shouldBe "A new var"
  }

  it should "support to set and use nested variables" in {
    val template =
      """{% set nested = ({"sub": {"label": "Label"}}) %}
        |A {{nested.sub.label}}
        |""".stripMargin
    lines(evaluate(template, Map.empty)) shouldBe Seq("A Label")
  }

  it should "be able to access global functions" in {
    val template = """{% for number in range(1, 3) %}
                     |   {{number}}
                     |{% endfor %}""".stripMargin
    lines(evaluate(template, Map.empty)) shouldBe Seq("1", "2")
  }

  it should "support sorting value" in {
    val template =
      """
        | {% for e in entities | sort(false, false, 'order') %}
        |   {{ e.item }}: {{ e.order }}
        | {% endfor %}
        |
        |""".stripMargin

    val values = Map(
      "entities"-> Seq(Map("item" -> "1", "order" -> "2").asJava, Map("item" -> "2", "order" -> "1").asJava).asJava
    )

    val expectedLines = Seq(
      "2: 1",
      "1: 2"
    )

    lines(evaluateRaw(template, values)) shouldBe expectedLines
  }

  it should "keep variable values literal instead of re-interpreting template syntax contained in them" in {
    val values = Seq(
      new TemplateVariableValue("test", VariableScope("execution"), Seq("test workflow")),
      new TemplateVariableValue("c", VariableScope("execution"), Seq("Some complex {{execution.test}}")),
      new TemplateVariableValue("stmt", VariableScope("execution"), Seq("{% set x = 'injected' %}{{x}}"))
    )
    val writer = new StringWriter()
    JinjaTemplateEngine().compile("{{execution.c}}|{{execution.stmt}}").evaluate(values, writer)
    writer.toString shouldBe "Some complex {{execution.test}}|{% set x = 'injected' %}{{x}}"
  }

  private def evaluate(template: String, values: Map[String, Seq[String]]): String = {
    val writer = new StringWriter()
    val compileTemplate = JinjaTemplateEngine().compile(template)
    val templateValues =
      for((name, value) <- values.toSeq) yield {
        new TemplateVariableValue(name, VariableScope.empty, value)
      }
    compileTemplate.evaluate(templateValues, writer)
    writer.toString
  }

  private def evaluateRaw(template: String, values: Map[String, AnyRef]): String = {
    val writer = new StringWriter()
    val compileTemplate = JinjaTemplateEngine().compile(template)
    compileTemplate.evaluate(values, writer: Writer)
    writer.toString
  }

  private def lines(str: String): Seq[String] = {
    ArraySeq.unsafeWrapArray(str.split("\\s*[\n\r]+\\s*").filter(_.nonEmpty))
  }

}
