package org.silkframework.plugins.dataset.rdf.tasks.templating

import org.apache.jena.vocabulary.XSD
import org.silkframework.plugins.templating.jinja.JinjaTemplateEngine
import org.silkframework.runtime.validation.ValidationException
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.runtime.templating.exceptions.TemplateEvaluationException

class SparqlUpdateTemplateJinjaTest extends AnyFlatSpec with Matchers {

  behavior of "SPARQL templating with the Jinja Template Engine"

  it should "output the correct input paths of the template" in {
    val templateString =
      """
        |{{ row.uri("subject") }}
        |{% if row.exists("somePath") %}
        |  Plain: {{ row.plainLiteral("somePath") }}
        |  Raw: {{ row.rawUnsafe("trustedValuePath") }}
        |{% endif %}
        |""".stripMargin
    val compiled = JinjaTemplateEngine().compile(templateString)
    compiled.variables.get.map(_.name).sorted mustBe Seq("row")
  }

  it should "validate without problems for valid templates" in {
    val templateWithLogic = s"""PREFIX xsd: <${XSD.getURI}>
                               |INSERT DATA {
                               |  <urn:entity:1> <urn:prop:1> "entity 1" .
                               |  {% if row.exists("input1") %}
                               |    {{ row.uri("input1") }} <urn:prop:2> {{ row.plainLiteral("input2") }}^^xsd:string
                               |  {% endif %}
                               |};
                               |""".stripMargin

    JinjaTemplateEngine().compile(templateWithLogic).variables.get.map(_.name).sorted mustBe Seq("row")
    validate(templateWithLogic)
  }

  it should "always validate templates as correct if rawUnsafe() is used, because there is no way to generate meaningful examples to validate" in {
    validate("""Completely broken SPARQL Update query with {{ row.rawUnsafe("something") }}""")
  }

  it should "raise a validation error when the template is invalid" in {
    intercept[ValidationException] {
      validate("""DELETE DATA { <urn:a:b> unknownPrefix:label "test" } ;""")
    }
    intercept[ValidationException] {
      validate(
        """PREFIX foaf: <http://xmlns.com/foaf/0.1/>
          |
          |WITH <http://example/addresses>
          |DELETE { ?person ?property ?value }
          |WHERE { ?person ?property ?value ; foaf:givenName 'Fred } ;""".stripMargin) // Missing closing ' for literal
    }
    val batchTemplate =
      s"""PREFIX xsd: <${XSD.getURI}>
         |INSERT DATA { <urn:a:b> <urn:c:d> "hello" } ;""".stripMargin
    validate(batchTemplate.dropRight(1), batchSize = 1) // Dropped ';' at the end, not batch supported, but batch size is 1
    intercept[ValidationException] {
      validate(batchTemplate.dropRight(1)) // Dropped ';' at the end, not batch supported
    }
  }

  it should "render uri() values as SPARQL URI syntax" in {
    val result = generate(
      """INSERT DATA { {{ row.uri("subject") }} <urn:prop:1> "value" } ;""",
      Map("subject" -> "urn:entity:1")
    )
    result must include("<urn:entity:1>")
  }

  it should "render plainLiteral() values as escaped SPARQL literals" in {
    val result = generate(
      """INSERT DATA { <urn:entity:1> <urn:prop:1> {{ row.plainLiteral("label") }} } ;""",
      Map("label" -> """hello "world"""")
    )
    result must include(""""hello \"world\""""")
  }

  it should "inject rawUnsafe() values verbatim without modification" in {
    val rawValue = "<urn:entity:1> <urn:prop:1> <urn:entity:2>"
    val result = generate(
      """INSERT DATA { {{ row.rawUnsafe("raw") }} } ;""",
      Map("raw" -> rawValue)
    )
    result must include(rawValue)
  }

  it should "include or exclude blocks based on exists()" in {
    val template =
      """INSERT DATA { {% if row.exists("x") %}<urn:entity:1> <urn:prop:1> "found" .{% endif %} } ;"""
    generate(template, Map("x" -> "urn:entity:1")) must include("found")
    generate(template, Map.empty) must not include "found"
  }

  it should "render inputProperties and outputProperties via TaskProperties" in {
    val result = generate(
      """INSERT DATA { {{ inputProperties.uri("x") }} <urn:prop:1> {{ outputProperties.uri("y") }} } ;""",
      assignments = Map.empty,
      taskProps = TaskProperties(Map("x" -> "urn:input:1"), Map("y" -> "urn:output:1"))
    )
    result must include("<urn:input:1>")
    result must include("<urn:output:1>")
  }

  it should "throw a TemplateExecutionException when uri() receives a non-URI value" in {
    intercept[TemplateEvaluationException] {
      generate(
        """INSERT DATA { {{ row.uri("subject") }} <urn:prop:1> "value" } ;""",
        Map("subject" -> "not a uri")
      )
    }
  }

  def generate(template: String, assignments: Map[String, String], taskProps: TaskProperties = TaskProperties(Map.empty, Map.empty)): String = {
    new SparqlUpdateTemplate(JinjaTemplateEngine().compile(template)).generate(assignments, taskProps)
  }

  def validate(template: String, batchSize: Int = 2): Unit = {
    new SparqlUpdateTemplate(JinjaTemplateEngine().compile(template)).validate(batchSize)
  }
}
