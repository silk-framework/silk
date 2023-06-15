package org.silkframework.plugins.dataset.rdf.sparql

import org.silkframework.plugins.dataset.rdf.tasks.templating.{Row, SparqlVelocityTemplating, TaskProperties, TemplateExecutionException}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class SparqlTemplatingTest extends AnyFlatSpec with Matchers {
  behavior of "SPARQL Templating"

  it should "render a simple Velocity template" in {
    val stringTemplate =
      """SELECT * WHERE {
        |  $row.uri("uriProp") rdfs:label $row.plainLiteral("stringProp")
        |}""".stripMargin
    val template = SparqlVelocityTemplating.createTemplate(stringTemplate)
    for(i <- 1 to 10) {
      val rendered = SparqlVelocityTemplating.renderTemplate(
        template, Row(Map("uriProp" -> s"http://entity$i", "stringProp" -> s"some label $i")), TaskProperties(Map.empty, Map.empty))
      rendered mustBe
          s"""SELECT * WHERE {
             |  <http://entity$i> rdfs:label "some label $i"
             |}""".stripMargin
    }
  }

  it should "render templates safely as long as safe methods are used, no injection attack possible" in {
    val template = executeTemplate("""$row.plainLiteral("var")""", Map("var" -> "\"Delete everything!!!\""))
    template mustBe "\"\\\"Delete everything!!!\\\"\""
  }

  it should "fail if the value for uri() is not an URI" in {
    intercept[TemplateExecutionException] {
      executeTemplate("""$row.uri("uri")""", Map("uri" -> "http:// broken Uri >"))
    }
  }

  it should "output a nice error message when there is a syntax error" in {

  }

  it should "throw exception when a non-available method or variable is used" in {
    intercept[TemplateExecutionException] {
      executeTemplate("""Not existing $test""", Map.empty)
    }
    intercept[TemplateExecutionException] {
      executeTemplate("""Not existing $row.notExisting("blah")""", Map("a" -> "A"))
    }
    intercept[TemplateExecutionException] {
      executeTemplate("""Not existing $row.uri("notExists")""", Map("a" -> "A"))
    }
  }

  private def executeTemplate(templateString: String, bindings: Map[String, String]): String = {
    val template = SparqlVelocityTemplating.createTemplate(templateString)
    SparqlVelocityTemplating.renderTemplate(template, Row(bindings), TaskProperties(Map.empty, Map.empty))
  }
}
