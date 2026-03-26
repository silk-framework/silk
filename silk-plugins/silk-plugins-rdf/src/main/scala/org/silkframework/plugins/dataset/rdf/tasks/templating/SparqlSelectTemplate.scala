package org.silkframework.plugins.dataset.rdf.tasks.templating

import org.silkframework.dataset.rdf.SparqlEndpoint
import org.silkframework.runtime.templating.{CompiledTemplate, TemplateEngines, TemplateVariableValue}

import java.io.StringWriter

/**
 * Wraps a [[CompiledTemplate]] and adds SPARQL SELECT specific capabilities.
 */
class SparqlSelectTemplate(template: CompiledTemplate) {

  /**
   * Evaluates the template and returns the SPARQL query.
   */
  def evaluate(endpoint: SparqlEndpoint): String = {
    evaluateWithVariables(Seq(
      new TemplateVariableValue(name = "graph", values = endpoint.sparqlParams.graph.toSeq)
    ))
  }

  /**
   * Evaluates the template using default values for the variables and returns the SPARQL query.
   */
  def evaluateWithDefaults(): String = {
    evaluateWithVariables(Seq(
      new TemplateVariableValue(name = "graph", values = Seq.empty)
    ))
  }

  private def evaluateWithVariables(variables: Seq[TemplateVariableValue]): String = {
    val writer = new StringWriter
    template.evaluate(variables, writer)
    writer.toString
  }

}

object SparqlSelectTemplate {

  /**
   * Creates a SPARQL template from a string.
   */
  def create(templateEngineId: String, template: String): SparqlSelectTemplate = {
    val templateEngine = TemplateEngines.create(templateEngineId)
    val sparqlTemplate = new SparqlSelectTemplate(templateEngine.compile(template))
    sparqlTemplate
  }
}
