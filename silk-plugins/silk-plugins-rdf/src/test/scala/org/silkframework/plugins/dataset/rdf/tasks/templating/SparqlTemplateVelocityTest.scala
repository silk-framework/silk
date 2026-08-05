package org.silkframework.plugins.dataset.rdf.tasks.templating

import org.apache.jena.vocabulary.XSD
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.plugins.templating.velocity.VelocityTemplateEngine
import org.silkframework.runtime.validation.ValidationException
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.runtime.templating.{InMemoryTemplateVariablesReader, TemplateVariables}
import org.silkframework.runtime.templating.exceptions.TemplateEvaluationException

class SparqlTemplateVelocityTest extends AnyFlatSpec with Matchers {

  behavior of "SPARQL templating with the Velocity Template Engine"

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
    val compiled = VelocityTemplateEngine().compile(templateString)
    compiled.variables.get.map(_.name).sorted mustBe Seq("row")
  }

  it should "validate without problems for valid templates" in {
    val templateWithLogic = s"""PREFIX xsd: <${XSD.getURI}>
                               |INSERT DATA {
                               |  <urn:entity:1> <urn:prop:1> "entity 1" .
                               |  #if ($$row.exists("input1"))
                               |    $$row.uri("input1") <urn:prop:2> $$row.plainLiteral("input2")^^xsd:string
                               |  #end
                               |};
                               |""".stripMargin

    validate(sparqlUpdateTemplate)
    VelocityTemplateEngine().compile(templateWithLogic).variables.get.map(_.name).sorted mustBe Seq("row")
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

  it should "not require an input if only outputProperties are referenced" in {
    val template = new SparqlLegacyTemplate(VelocityTemplateEngine().compile(
      """INSERT DATA { <urn:s:1> <urn:p:graph> $outputProperties.uri("graph") } ;"""))
    template.requiresInput mustBe false
    template.inputSchema.typedPaths mustBe empty
    val rendered = template.generate(None, TaskProperties(Map.empty, Map("graph" -> "urn:graph:g1"))).head
    rendered mustBe """INSERT DATA { <urn:s:1> <urn:p:graph> <urn:graph:g1> } ;"""
    // Entity values and inputProperties still require an input connection
    new SparqlLegacyTemplate(VelocityTemplateEngine().compile(
      """INSERT DATA { <urn:s:1> <urn:p:graph> $inputProperties.uri("graph") } ;""")).requiresInput mustBe true
    new SparqlLegacyTemplate(VelocityTemplateEngine().compile(
      """INSERT DATA { $row.uri("subject") <urn:p:graph> $outputProperties.uri("graph") } ;""")).requiresInput mustBe true
  }

  it should "not silently drop an outputProperties-only reference that uses an undocumented method name" in {
    val template = new SparqlLegacyTemplate(VelocityTemplateEngine().compile(
      """INSERT DATA { <urn:s:1> <urn:p:graph> $outputProperties.get("graph") } ;"""))
    template.requiresInput mustBe false
  }

  it should "still require input correctly when a bare outputProperties reference coexists with a proper row usage" in {
    val template = new SparqlLegacyTemplate(VelocityTemplateEngine().compile(
      """#if($outputProperties)INSERT DATA { $row.uri("subject") <urn:p:graph> "static" } ;#end"""))
    template.requiresInput mustBe true
  }

  it should "not require an input for a fully static template referencing none of row/inputProperties/outputProperties" in {
    val template = new SparqlLegacyTemplate(VelocityTemplateEngine().compile(
      """INSERT DATA { <urn:s:1> <urn:p:graph> <urn:o:1> } ;"""))
    template.requiresInput mustBe false
    template.inputSchema.typedPaths mustBe empty
  }

  it should "render a simple Velocity template" in {
    val stringTemplate =
      """SELECT * WHERE {
        |  $row.uri("uriProp") rdfs:label $row.plainLiteral("stringProp")
        |}""".stripMargin
    val template = new SparqlLegacyTemplate(VelocityTemplateEngine().compile(stringTemplate))
    for(i <- 1 to 10) {
      val rendered = template.generate(Some(entityFromMap(Map("uriProp" -> s"http://entity$i", "stringProp" -> s"some label $i"))), TaskProperties(Map.empty, Map.empty)).head
      rendered mustBe
        s"""SELECT * WHERE {
           |  <http://entity$i> rdfs:label "some label $i"
           |}""".stripMargin
    }
  }

  it should "render templates safely as long as safe methods are used, no injection attack possible" in {
    val template = generate("""$row.plainLiteral("var")""", Map("var" -> "\"Delete everything!!!\""))
    template mustBe "\"\\\"Delete everything!!!\\\"\""
  }

  it should "fail if the value for uri() is not an URI" in {
    intercept[TemplateEvaluationException] {
      generate("""$row.uri("uri")""", Map("uri" -> "http:// broken Uri >"))
    }
  }

  it should "throw exception when a non-available method or variable is used" in {
    intercept[TemplateEvaluationException] {
      generate("""Not existing $test""", Map.empty)
    }
    intercept[TemplateEvaluationException] {
      generate("""Not existing $row.notExisting("blah")""", Map("a" -> "A"))
    }
    intercept[TemplateEvaluationException] {
      generate("""Not existing $row.uri("notExists")""", Map("a" -> "A"))
    }
  }

  private def generate(templateString: String, bindings: Map[String, String]): String = {
    val entity = if (bindings.isEmpty) None else Some(entityFromMap(bindings))
    new SparqlLegacyTemplate(VelocityTemplateEngine().compile(templateString)).generate(entity, TaskProperties(Map.empty, Map.empty)).head
  }

  private def entityFromMap(values: Map[String, String]): Entity = {
    val entries = values.toIndexedSeq
    val schema = EntitySchema("", entries.map { case (k, _) => UntypedPath(k).asUntypedValueType })
    Entity("urn:test", entries.map { case (_, v) => Seq(v) }, schema)
  }

  def validate(template: String, batchSize: Int = 2): Unit = {
    new SparqlLegacyTemplate(VelocityTemplateEngine().compile(template)).validate(InMemoryTemplateVariablesReader(TemplateVariables.empty, Set.empty), Some(batchSize))
  }
}
