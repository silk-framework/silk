package org.silkframework.plugins.dataset.rdf.tasks.templating

import org.apache.jena.vocabulary.XSD
import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.runtime.validation.ValidationException

class SparqlTemplatingEngineVelocityTest extends FlatSpec with MustMatchers {
  behavior of "Velocity SPARQL Templating Engine"

  private val sparqlUpdateTemplate =
    s"""PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
       |PREFIX xsd: <${XSD.getURI}>
       |DELETE DATA { $$row.uri("PROP_FROM_ENTITY_SCHEMA1") rdf:label $$row.plainLiteral("PROP_FROM_ENTITY_SCHEMA2") } ;
       |INSERT DATA { $$row.uri("PROP_FROM_ENTITY_SCHEMA1") rdf:label $$row.plainLiteral("PROP_FROM_ENTITY_SCHEMA3")^^xsd:int } ;""".stripMargin

  it should "output the correct input paths of the template" in {
    val templateString =
      """
        |$row.uri("subject")
        |#if ( $row.exists("somePath") )
        |  Plain: $row.plainLiteral("somePath")
        |  Raw: $row.rawUnsafe("trustedValuePath")
        |#end
        |""".stripMargin
    val engine = SparqlTemplatingEngineVelocity(templateString, 1)
    engine.inputPaths().sorted mustBe Seq("somePath", "subject", "trustedValuePath")
  }

  private val templateWithLogic = s"""PREFIX xsd: <${XSD.getURI}>
                                    |INSERT DATA {
                                    |  <urn:entity:1> <urn:prop:1> "entity 1" .
                                    |  #if ($$row.exists("input1"))
                                    |    $$row.uri("input1") <urn:prop:2> $$row.plainLiteral("input2")^^xsd:string
                                    |  #end
                                    |};
                                    |""".stripMargin

  it should "validate without problems for valid templates" in {
    validate(sparqlUpdateTemplate)
    SparqlTemplatingEngineVelocity(templateWithLogic, 1).inputPaths().sorted mustBe Seq("input1", "input2")
    validate(templateWithLogic)
  }

  it should "always validate templates as correct if rawUnsafe() is used, because there is no way to generate meaningful examples to validate" in {
    validate("""Completely broken SPARQL Update query with $row.rawUnsafe("something")""")
  }

  it should "raise a validation error when the template is invalid" in {
    intercept[ValidationException] {
      validate("""DELETE DATA { $row.uri("test") rdf:label } ;""")
    }
    intercept[ValidationException] {
      validate("""DELETE DATA { <urn:a:b> rdf:label $row.uri(3) ;""")
    }
    intercept[ValidationException] {
      // No rdf prefix defined
      validate("""DELETE DATA { $row.uri("PROP_FROM_ENTITY_SCHEMA1") rdf:label $row.plainLiteral("PROP_FROM_ENTITY_SCHEMA2") } ;
              |  INSERT DATA { $row.uri("PROP_FROM_ENTITY_SCHEMA1") rdf:label $row.plainLiteral("PROP_FROM_ENTITY_SCHEMA3") } ;""".stripMargin)
    }
    intercept[ValidationException] {
      validate("""PREFIX foaf:  <http://xmlns.com/foaf/0.1/>
              |
              |WITH <http://example/addresses>
              |DELETE { ?person ?property ?value }
              |WHERE { ?person ?property ?value ; foaf:givenName 'Fred } ;""".stripMargin) // Missing closing ' for literal
    }
    validate(sparqlUpdateTemplate.dropRight(1), batchSize = 1) // Dropped ';' at the end, not batch supported, but batch size is 1
    intercept[ValidationException] {
      validate(sparqlUpdateTemplate.dropRight(1)) // Dropped ';' at the end, not batch supported
    }
  }

  def validate(template: String, batchSize: Int = 2): Unit = {
    SparqlTemplatingEngineVelocity(template, batchSize).validate()
  }
}
