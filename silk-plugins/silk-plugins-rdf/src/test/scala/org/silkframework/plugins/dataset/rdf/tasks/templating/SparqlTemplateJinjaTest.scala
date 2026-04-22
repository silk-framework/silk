package org.silkframework.plugins.dataset.rdf.tasks.templating

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.plugins.templating.jinja.JinjaTemplateEngine
import org.silkframework.runtime.templating.TemplateVariableValue
import org.silkframework.runtime.templating.exceptions.{TemplateEvaluationException, UnboundVariablesException}

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

  it should "render a realistic SPARQL Update template combining all variable scopes, filters and a conditional" in {
    val templateString =
      """PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
        |WITH <{{ output.config.graph | validate_uri }}>
        |DELETE { <{{ input.entity.subject | validate_uri }}> ?p ?o }
        |INSERT {
        |  <{{ input.entity.subject | validate_uri }}> rdfs:label "{{ input.entity.label | escape_literal }}" .
        |  <{{ input.entity.subject | validate_uri }}> <{{ project.labelProp | validate_uri }}> "{{ global.author | escape_literal }}" .
        |  {% if input.entity.comment %}
        |  <{{ input.entity.subject | validate_uri }}> rdfs:comment '''{{ input.entity.comment | escape_multiline_literal }}''' .
        |  {% endif %}
        |}
        |WHERE { <{{ input.entity.subject | validate_uri }}> ?p ?o } ;
        |""".stripMargin
    val template = SparqlTemplate.create(JinjaTemplateEngine.id, templateString)

    template.inputSchema.typedPaths.flatMap(_.property).map(_.propertyUri.uri).toSet mustBe
      Set("subject", "label", "comment")
    template.isStaticTemplate mustBe false

    val taskProps = TaskProperties(inputTask = Map.empty, outputTask = Map("graph" -> "urn:graph:out"))
    val projectAndGlobal = Seq(
      new TemplateVariableValue("labelProp", Seq("project"), Seq("urn:prop:label")),
      new TemplateVariableValue("author", Seq("global"), Seq("Jane"))
    )

    val rendered = template.generate(
      placeholderAssignments = Map(
        "subject" -> "urn:entity:1",
        "label" -> """O'Reilly & "friends"""",
        "comment" -> "has ''' triple quotes"
      ),
      taskProperties = taskProps,
      templateVariables = projectAndGlobal
    )

    rendered must include("WITH <urn:graph:out>")
    rendered must include("<urn:entity:1>")
    rendered must include(""""O\'Reilly & \"friends\""""")
    rendered must include("<urn:prop:label>")
    rendered must include(""""Jane"""")
    rendered must include("""has \'\'\' triple quotes""")

    // With an empty `comment`, the {% if %} branch is skipped.
    val withoutComment = template.generate(
      placeholderAssignments = Map("subject" -> "urn:entity:1", "label" -> "plain", "comment" -> ""),
      taskProperties = taskProps,
      templateVariables = projectAndGlobal
    )
    withoutComment must not include "rdfs:comment"

    // An invalid IRI piped through validate_uri surfaces as a TemplateEvaluationException.
    intercept[TemplateEvaluationException] {
      template.generate(
        placeholderAssignments = Map("subject" -> "not a uri", "label" -> "plain", "comment" -> ""),
        taskProperties = taskProps,
        templateVariables = projectAndGlobal
      )
    }
  }

  private def generate(template: String,
                       assignments: Map[String, String] = Map.empty,
                       taskProps: TaskProperties = TaskProperties(Map.empty, Map.empty),
                       templateVariables: Seq[TemplateVariableValue] = Seq.empty): String = {
    SparqlTemplate.create(JinjaTemplateEngine.id, template).generate(assignments, taskProps, templateVariables)
  }
}
