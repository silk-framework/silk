package org.silkframework.plugins.dataset.rdf.tasks.templating

import org.silkframework.entity.paths.{TypedPath, UntypedPath}
import org.silkframework.entity.{Entity, EntitySchema, ValueType}
import org.silkframework.execution.local.EmptyEntityTable
import org.silkframework.runtime.templating.{CompiledTemplate, TemplateEngines, TemplateVariableConversions, TemplateVariableName, TemplateVariableValue, TemplateVariablesReader}
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Uri

import java.io.StringWriter

/**
  * SPARQL template implementation for the Jinja engine.
  *
  * Exposes the following variables:
  *
  *   {{ input.config.<param>    }} -- parameter of the connected input task
  *   {{ input.entity.<property> }} -- value of the current input entity
  *   {{ output.config.<param>   }} -- parameter of the connected output task
  *   {{ project.<key>           }} -- project-scoped template variable
  *   {{ global.<key>            }} -- global template variable
  *
  * Entity property names must be valid Jinja identifiers (`[a-zA-Z_][a-zA-Z0-9_]*`)
  *
  * @param rawTemplate  The raw template source. Compiled internally via the Jinja engine and also used
  *                     to derive the output schema heuristically without rendering.
  * @param defaultScope If non-empty, every variable at this scope is also exposed at the top level of the
  *                     Jinja context, so the template may reference it without the scope prefix. For example,
  *                     with `defaultScope = Seq("input", "entity")`, a template may use `{{ property }}` in
  *                     place of `{{ input.entity.property }}`.
  */
class SparqlJinjaTemplate(rawTemplate: String, defaultScope: Seq[String] = Seq.empty) extends SparqlTemplate {

  import SparqlJinjaTemplate._

  private val template: CompiledTemplate = TemplateEngines.create(JINJA_ENGINE_ID).compile(rawTemplate)

  override def generate(entity: Option[Entity],
                        taskProperties: TaskProperties,
                        templateVariables: Seq[TemplateVariableValue] = Seq.empty): Iterable[String] = {
    val values = buildValues(entity, taskProperties, templateVariables)
    val writer = new StringWriter()
    template.evaluate(values, writer)
    Seq(writer.toString)
  }

  override def validate(variables: TemplateVariablesReader, batchSize: Option[Int]): Unit = {
    val available = variables.all.variables.map(v => (v.name, v.scope)).toSet
    for (variable <- referencedVariables.distinct) {
      val effectiveScope = if (variable.scope.isEmpty) defaultScope else variable.scope
      validateReference(variable, effectiveScope, available)
    }
  }

  private def validateReference(variable: TemplateVariableName,
                                effectiveScope: Seq[String],
                                available: Set[(String, Seq[String])]): Unit = {
    effectiveScope.headOption match {
      case Some(top) if VARIABLE_SCOPES.contains(top) =>
        if (!available.contains((variable.name, effectiveScope))) {
          throw new ValidationException(s"Unknown template variable '${variable.scopedName}'.")
        }
      case Some(top) if TASK_SCOPES.contains(top) =>
        val subSection = effectiveScope.lift(1).getOrElse("")
        if (!TASK_SUB_SECTIONS.contains(subSection)) {
          throw new ValidationException(
            s"Invalid template variable '${variable.scopedName}'. " +
              s"Only '$top.config.<param>' and '$top.entity.<param>' are valid.")
        }
      case _ =>
    }
  }

  override def inputSchema: EntitySchema = {
    val properties = entityPropertyNames
    if (properties.isEmpty) {
      EmptyEntityTable.schema
    } else {
      EntitySchema("", properties.map(p => UntypedPath(p).asUntypedValueType).toIndexedSeq)
    }
  }

  override lazy val outputSchema: EntitySchema = {
    val vars = SparqlSelectVarExtractor.extractSelectVars(rawTemplate)
    val paths = vars.map(v => TypedPath(UntypedPath(v), ValueType.STRING, isAttribute = false))
    EntitySchema(typeUri = Uri(""), typedPaths = paths.toIndexedSeq)
  }

  override def isStaticTemplate: Boolean = {
    entityPropertyNames.isEmpty
  }

  private def buildValues(entity: Option[Entity],
                          taskProperties: TaskProperties,
                          templateVariables: Seq[TemplateVariableValue]): Seq[TemplateVariableValue] = {
    val inputConfig = taskProperties.inputTask.map { case (k, v) =>
      new TemplateVariableValue(k, INPUT_CONFIG_SCOPE, Seq(v))
    }
    val inputEntity = entity.toSeq.flatMap(e => TemplateVariableConversions.fromEntity(e, INPUT_ENTITY_SCOPE))
    val outputConfig = taskProperties.outputTask.map { case (k, v) =>
      new TemplateVariableValue(k, OUTPUT_CONFIG_SCOPE, Seq(v))
    }
    val scoped = (inputConfig ++ inputEntity ++ outputConfig).toSeq ++ templateVariables
    val aliased =
      if (defaultScope.nonEmpty) {
        scoped.filter(_.scope == defaultScope).map(v => new TemplateVariableValue(v.name, Seq.empty, v.values))
      } else {
        Seq.empty
      }
    scoped ++ aliased
  }

  private def referencedVariables: Seq[TemplateVariableName] = {
    template.variables.getOrElse(Seq.empty)
  }

  private def entityPropertyNames: Seq[String] = {
    val scoped = referencedVariables.filter(_.scope == INPUT_ENTITY_SCOPE).map(_.name)
    val aliased =
      if (defaultScope == INPUT_ENTITY_SCOPE) {
        referencedVariables.filter(_.scope.isEmpty).map(_.name)
      } else {
        Seq.empty
      }
    (scoped ++ aliased).distinct
  }
}

object SparqlJinjaTemplate {

  private[templating] final val JINJA_ENGINE_ID = "jinja"

  private[templating] final val INPUT_CONFIG_SCOPE: Seq[String] = Seq("input", "config")
  private[templating] final val INPUT_ENTITY_SCOPE: Seq[String] = Seq("input", "entity")
  private[templating] final val OUTPUT_CONFIG_SCOPE: Seq[String] = Seq("output", "config")

  private final val VARIABLE_SCOPES: Set[String] = Set("project", "global")
  private final val TASK_SCOPES: Set[String] = Set("input", "output")
  private final val TASK_SUB_SECTIONS: Set[String] = Set("config", "entity")
}
