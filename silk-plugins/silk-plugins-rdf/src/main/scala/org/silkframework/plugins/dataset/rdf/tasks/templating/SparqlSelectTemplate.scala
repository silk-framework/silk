package org.silkframework.plugins.dataset.rdf.tasks.templating

import org.silkframework.config.Task
import org.silkframework.dataset.DatasetSpec
import org.silkframework.dataset.rdf.RdfDataset
import org.silkframework.plugins.dataset.rdf.tasks.templating.SparqlSelectTemplate.inputTaskScope
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.templating.TemplateVariableConversions._
import org.silkframework.runtime.templating.{CompiledTemplate, TemplateEngines, TemplateVariableValue}

import java.io.StringWriter

/**
 * Wraps a [[CompiledTemplate]] and adds SPARQL SELECT specific capabilities.
 */
class SparqlSelectTemplate(template: CompiledTemplate) {

  /**
   * Evaluates the template and returns the SPARQL query.
   */
  def evaluate(task: Task[DatasetSpec[RdfDataset]])(implicit pluginContext: PluginContext): String = {
    evaluateWithVariables(
      fromTask(task, scope = inputTaskScope)
    )
  }

  /**
   * Evaluates the template using default values for the variables and returns the SPARQL query.
   * All unassigned variables are filled with an empty string.
   */
  def evaluateWithDefaults(): String = {
    val defaultVariables = template.variables.getOrElse(Seq.empty).map { varName =>
      new TemplateVariableValue(varName.name, varName.scope, Seq(""))
    }
    evaluateWithVariables(defaultVariables)
  }

  private def evaluateWithVariables(variables: Seq[TemplateVariableValue]): String = {
    val writer = new StringWriter
    template.evaluate(variables, writer)
    writer.toString
  }

}

object SparqlSelectTemplate {

  val inputTaskScope: Seq[String] = Seq("input")

  /**
   * Creates a SPARQL template from a string.
   */
  def create(templateEngineId: String, template: String): SparqlSelectTemplate = {
    val templateEngine = TemplateEngines.create(templateEngineId)
    val sparqlTemplate = new SparqlSelectTemplate(templateEngine.compile(template))
    sparqlTemplate
  }
}
