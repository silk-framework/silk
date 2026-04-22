package org.silkframework.plugins.dataset.rdf.tasks.templating

import org.apache.jena.update.UpdateFactory
import org.silkframework.config.{Prefixes, Task, TaskSpec}
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.templating.{TemplateEngines, TemplateVariableValue}
import org.silkframework.runtime.validation.ValidationException

import scala.util.Try

/**
  * Compiled SPARQL template. Encapsulates rendering a SPARQL query from a template and the associated
  * context (connected input/output task properties, current entity values, project/global variables).
  *
  * Two concrete implementations exist:
  *
  * - [[SparqlJinjaTemplate]] for the Jinja engine, which exposes variables as `input.config.*`,
  *   `input.entity.*`, `output.config.*`, `project.*` and `global.*`.
  * - [[SparqlLegacyTemplate]] for the Velocity and Simple engines, which exposes the historical
  *   `row` / `inputProperties` / `outputProperties` object API.
  */
trait SparqlTemplate {

  /**
   * Renders the template.
   *
   * @param entity            The current input entity, or `None` for static templates / Select queries.
   *                          The Jinja implementation exposes each entity property as a list of values
   *                          under `input.entity.*`. The legacy implementation iterates over the
   *                          cross-product of property values and emits one query per combination.
   * @param taskProperties    Parameter values of the connected input and output tasks.
   * @param templateVariables Project and global template variables (scoped as `Seq("project")` / `Seq("global")`).
   *                          Only used by the Jinja implementation; the legacy implementation ignores them.
   * @return One rendered query for Jinja, or one query per cross-product combination for the legacy engine.
   */
  def generate(entity: Option[Entity],
               taskProperties: TaskProperties,
               templateVariables: Seq[TemplateVariableValue] = Seq.empty): Iterable[String]

  /** Renders the template with example values for every variable. Used to derive schemas and validate queries. */
  def generateWithDefaults(): String

  /** Validates the template and, if batchSize > 1, that batching produces valid SPARQL. */
  def validateUpdateQuery(batchSize: Int): Unit

  /** Entity schema that the template expects on its input port. */
  def inputSchema: EntitySchema

  /** True if the template does not reference any entity values and thus needs no input port. */
  def isStaticTemplate: Boolean
}

object SparqlTemplate {

  // Must match JinjaTemplateEngine.id. Duplicated here because silk-plugins-rdf does not depend on
  // silk-plugins-templating-jinja at compile time (only at test scope).
  private final val JINJA_ENGINE_ID = "jinja"

  /**
   * Creates a SPARQL template using the given template engine.
   */
  def create(templateEngineId: String, template: String): SparqlTemplate = {
    val engine = TemplateEngines.create(templateEngineId)
    val compiled = engine.compile(template)
    if (templateEngineId == JINJA_ENGINE_ID) {
      new SparqlJinjaTemplate(compiled)
    } else {
      new SparqlLegacyTemplate(compiled)
    }
  }

  /**
   * Verifies that a rendered example query parses as SPARQL Update, and — when batchSize > 1 — that two
   * consecutive copies also parse (so batching in [[org.silkframework.plugins.dataset.rdf.executors.BatchSparqlUpdateEmitter]]
   * produces valid queries).
   *
   * Shared between [[SparqlJinjaTemplate]] and [[SparqlLegacyTemplate]].
   */
  private[templating] def validateParseability(query: String, batchSize: Int): Unit = {
    Try(UpdateFactory.create(query)).failed.toOption.foreach { parseError =>
      throw new ValidationException(
        "The SPARQL Update template does not generate valid SPARQL Update queries. Error message: " +
          parseError.getMessage + ", example query: " + query)
    }
    if (batchSize > 1) {
      val batchSparql = query + "\n" + query
      Try(UpdateFactory.create(batchSparql)).failed.toOption.foreach { parseError =>
        throw new ValidationException(
          "The SPARQL Update template cannot be batched processed. There is probably a ';' missing at the end. Error message: " +
            parseError.getMessage + ", example batch query: " + batchSparql)
      }
    }
  }
}

/** Makes properties of the input and output task of a SPARQL operator execution available to the template. */
case class TaskProperties(inputTask: Map[String, String], outputTask: Map[String, String])

object TaskProperties {

  def create(inputTask: Option[Task[_ <: TaskSpec]],
             outputTask: Option[Task[_ <: TaskSpec]],
             pluginContext: PluginContext): TaskProperties = {
    // It's obligatory to have empty prefixes here, since we do not want to have prefixed URIs for URI parameters
    implicit val updatedPluginContext: PluginContext = PluginContext.updatedPluginContext(pluginContext, prefixes = Some(Prefixes.empty))
    val inputProperties = createTaskProperties(inputTask)
    val outputProperties = createTaskProperties(outputTask)
    TaskProperties(inputProperties, outputProperties)
  }

  private def createTaskProperties(task: Option[Task[_ <: TaskSpec]])
                                  (implicit pluginContext: PluginContext): Map[String, String] = {
    task.toSeq.flatMap(_.parameters.toStringMap).toMap
  }
}
