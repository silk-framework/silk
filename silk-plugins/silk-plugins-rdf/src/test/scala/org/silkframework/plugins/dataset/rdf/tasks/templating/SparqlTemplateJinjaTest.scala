package org.silkframework.plugins.dataset.rdf.tasks.templating

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.plugins.templating.jinja.JinjaTemplateEngine
import org.silkframework.runtime.templating.TemplateVariableValue
import org.silkframework.runtime.templating.exceptions.UnboundVariablesException

class SparqlTemplateJinjaTest extends AnyFlatSpec with Matchers {

  behavior of "SPARQL templating with the Jinja Template Engine"

  it should "render values from the current input entity via input.entity" in {
    val result = generate(
      """INSERT DATA { <{{ input.entity.subject }}> <urn:prop:1> "value" } ;""",
      assignments = Map("subject" -> "urn:entity:1")
    )
    result must include("<urn:entity:1>")
  }

  it should "render parameters of the connected input task via input.config" in {
    val result = generate(
      """SELECT * WHERE { GRAPH <{{ input.config.graph }}> { ?s ?p ?o } }""",
      taskProps = TaskProperties(Map("graph" -> "urn:graph:1"), Map.empty)
    )
    result must include("<urn:graph:1>")
  }

  it should "render parameters of the connected output task via output.config" in {
    val result = generate(
      """INSERT DATA { GRAPH <{{ output.config.graph }}> { <urn:a> <urn:b> <urn:c> } } ;""",
      taskProps = TaskProperties(Map.empty, Map("graph" -> "urn:graph:out"))
    )
    result must include("<urn:graph:out>")
  }

  it should "render project and global template variables" in {
    val project = new TemplateVariableValue("myVar", Seq("project"), Seq("projectValue"))
    val global = new TemplateVariableValue("myVar", Seq("global"), Seq("globalValue"))
    val result = generate(
      """{{ project.myVar }} / {{ global.myVar }}""",
      templateVariables = Seq(project, global)
    )
    result must include("projectValue / globalValue")
  }

  it should "reject old Jinja syntax (row, inputProperties, outputProperties)" in {
    intercept[UnboundVariablesException] {
      generate("""{{ row.uri("x") }}""", assignments = Map("x" -> "urn:a:b"))
    }
    intercept[UnboundVariablesException] {
      generate("""{{ inputProperties.uri("graph") }}""",
        taskProps = TaskProperties(Map("graph" -> "urn:g:1"), Map.empty))
    }
  }

  it should "derive the input schema from input.entity.* references" in {
    val template = SparqlTemplate.create(JinjaTemplateEngine.id,
      """
        |INSERT DATA {
        |  <{{ input.entity.subject }}> <urn:prop:1> "{{ input.entity.label }}" .
        |  <{{ input.entity.subject }}> <urn:prop:2> "value" .
        |} ;
        |""".stripMargin)
    val paths = template.inputSchema.typedPaths.flatMap(_.property).map(_.propertyUri.uri)
    paths.toSet mustBe Set("subject", "label")
  }

  it should "report a Jinja template with no entity references as static" in {
    val staticTemplate = SparqlTemplate.create(JinjaTemplateEngine.id,
      """INSERT DATA { <{{ input.config.target }}> <urn:p> "v" } ;""")
    staticTemplate.isStaticTemplate mustBe true
    val dynamicTemplate = SparqlTemplate.create(JinjaTemplateEngine.id,
      """INSERT DATA { <{{ input.entity.subject }}> <urn:p> "v" } ;""")
    dynamicTemplate.isStaticTemplate mustBe false
  }

  private def generate(template: String,
                       assignments: Map[String, String] = Map.empty,
                       taskProps: TaskProperties = TaskProperties(Map.empty, Map.empty),
                       templateVariables: Seq[TemplateVariableValue] = Seq.empty): String = {
    SparqlTemplate.create(JinjaTemplateEngine.id, template).generate(assignments, taskProps, templateVariables)
  }
}
