package org.silkframework.plugins.dataset.rdf.tasks.templating

import org.apache.jena.vocabulary.XSD
import org.silkframework.plugins.templating.jinja.JinjaTemplateEngine
import org.silkframework.runtime.validation.ValidationException
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class SparqlTemplateJinjaTest extends AnyFlatSpec with Matchers {

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

  def validate(template: String, batchSize: Int = 2): Unit = {
    new SparqlTemplate(JinjaTemplateEngine().compile(template)).validate(batchSize)
  }
}
