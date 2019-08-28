package org.silkframework.plugins.dataset.rdf

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.plugins.dataset.rdf.tasks._
import org.silkframework.plugins.dataset.rdf.tasks.templating._
import org.silkframework.runtime.validation.ValidationException

class SparqlUpdateTemplatingEngineSimpleTest extends FlatSpec with MustMatchers {
  behavior of "SPARQL Update Simple Templating Engine"

  private val sparqlUpdateTemplate =
    """PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
      |DELETE DATA { ${<PROP_FROM_ENTITY_SCHEMA1>} rdf:label ${"PROP_FROM_ENTITY_SCHEMA2"} } ;
      |  INSERT DATA { ${<PROP_FROM_ENTITY_SCHEMA1>} rdf:label ${"PROP_FROM_ENTITY_SCHEMA3"} } ;""".stripMargin

  it should "parse the SPARQL Update template correctly" in {
    parse(sparqlUpdateTemplate) mustBe Seq(
      SparqlUpdateTemplateStaticPart("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\nDELETE DATA { "),
      SparqlUpdateTemplateURIPlaceholder("PROP_FROM_ENTITY_SCHEMA1"),
      SparqlUpdateTemplateStaticPart(" rdf:label "),
      SparqlUpdateTemplatePlainLiteralPlaceholder("PROP_FROM_ENTITY_SCHEMA2"),
      SparqlUpdateTemplateStaticPart(" } ;\n  INSERT DATA { "),
      SparqlUpdateTemplateURIPlaceholder("PROP_FROM_ENTITY_SCHEMA1"),
      SparqlUpdateTemplateStaticPart(" rdf:label "),
      SparqlUpdateTemplatePlainLiteralPlaceholder("PROP_FROM_ENTITY_SCHEMA3"),
      SparqlUpdateTemplateStaticPart(" } ;")
    )
  }

  it should "raise a validation error when the template is invalid" in {
    intercept[ValidationException] {
      parse("""DELETE DATA { ${<${"PROP_FROM_ENTITY_SCHEMA2"}>} rdf:label "label" } ;""")
    }
    intercept[ValidationException] {
      parse("""DELETE DATA { <urn:a:b> rdf:label ${"${<PROP_FROM_ENTITY_SCHEMA1>}"} } ;""")
    }
    intercept[ValidationException] {
      // No rdf prefix defined
      parse("""DELETE DATA { ${<PROP_FROM_ENTITY_SCHEMA1>} rdf:label ${"PROP_FROM_ENTITY_SCHEMA2"} } ;
              |  INSERT DATA { ${<PROP_FROM_ENTITY_SCHEMA1>} rdf:label ${"PROP_FROM_ENTITY_SCHEMA3"} } ;""".stripMargin)
    }
    intercept[ValidationException] {
      parse("""PREFIX foaf:  <http://xmlns.com/foaf/0.1/>
              |
              |WITH <http://example/addresses>
              |DELETE { ?person ?property ?value }
              |WHERE { ?person ?property ?value ; foaf:givenName 'Fred } ;""".stripMargin) // Missing closing ' for literal
    }
    parse(sparqlUpdateTemplate.dropRight(1), batchSize = 1) // Dropped ';' at the end, not batch supported, but batch size is 1
    intercept[ValidationException] {
      parse(sparqlUpdateTemplate.dropRight(1)) // Dropped ';' at the end, not batch supported
    }
  }

  it should "generate the correct input schema from the template" in {
    val is = SparqlUpdateCustomTask(sparqlUpdateTemplate).inputSchemataOpt
    is.get.size mustBe 1
    is.get.head.typedPaths.flatMap(_.propertyUri).map(_.uri).toSet mustBe Set(
      "PROP_FROM_ENTITY_SCHEMA1",
      "PROP_FROM_ENTITY_SCHEMA2",
      "PROP_FROM_ENTITY_SCHEMA3"
    )
  }

  it should "generate the correct SPARQL Update query from the template" in {
    SparqlUpdateCustomTask(sparqlUpdateTemplate).generate(Map(
      "PROP_FROM_ENTITY_SCHEMA1" -> "urn:some:uri",
      "PROP_FROM_ENTITY_SCHEMA2" -> "the old label",
      "PROP_FROM_ENTITY_SCHEMA3" ->
        """The new
          |label with some "'weird characters""".stripMargin
    )) mustBe
      """PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
        |DELETE DATA { <urn:some:uri> rdf:label "the old label" } ;
        |  INSERT DATA { <urn:some:uri> rdf:label "The new\nlabel with some \"'weird characters" } ;""".stripMargin
  }

  def parse(sparqlUpdateTemplate: String, batchSize: Int = SparqlUpdateCustomTask.defaultBatchSize): Seq[SparqlUpdateTemplatePart] = {
    val engine = SparqlUpdateTemplatingEngineSimple(sparqlUpdateTemplate, batchSize)
    engine.validate()
    engine.sparqlUpdateTemplateParts
  }
}
