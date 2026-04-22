package org.silkframework.plugins.dataset.rdf.tasks.templating

import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.execution.local.EmptyEntityTable
import org.silkframework.runtime.templating.{CompiledTemplate, TemplateVariableConversions, TemplateVariableName, TemplateVariableValue}
import org.silkframework.runtime.validation.ValidationException

import java.io.StringWriter
import scala.util.{Failure, Success, Try}

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
  */
class SparqlJinjaTemplate(template: CompiledTemplate) extends SparqlTemplate {

  import SparqlJinjaTemplate._

  override def generate(entity: Option[Entity],
                        taskProperties: TaskProperties,
                        templateVariables: Seq[TemplateVariableValue] = Seq.empty): Iterable[String] = {
    val values = buildValues(entity, taskProperties, templateVariables)
    val writer = new StringWriter()
    template.evaluate(values, writer)
    Seq(writer.toString)
  }

  override def generateWithDefaults(): String = {
    // Seed every referenced variable with a URI-like default so that QueryFactory can parse the result.
    val genericUri = "urn:generic:1"
    val defaults = referencedVariables.distinct.map(v => new TemplateVariableValue(v.name, v.scope, Seq(genericUri)))
    Try {
      val writer = new StringWriter()
      template.evaluate(defaults, writer)
      writer.toString
    } match {
      case Success(query) => query
      case Failure(exception) =>
        throw new ValidationException(
          "The SPARQL Update template could not be rendered with example values. Error message: " + exception.getMessage, exception)
    }
  }

  override def validateUpdateQuery(batchSize: Int): Unit = {
    SparqlTemplate.validateParseability(generateWithDefaults(), batchSize)
  }

  override def inputSchema: EntitySchema = {
    val properties = entityPropertyNames
    if (properties.isEmpty) {
      EmptyEntityTable.schema
    } else {
      EntitySchema("", properties.map(p => UntypedPath(p).asUntypedValueType).toIndexedSeq)
    }
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
    (inputConfig ++ inputEntity ++ outputConfig).toSeq ++ templateVariables
  }

  private def referencedVariables: Seq[TemplateVariableName] = {
    template.variables.getOrElse(Seq.empty)
  }

  private def entityPropertyNames: Seq[String] = {
    referencedVariables.filter(_.scope == INPUT_ENTITY_SCOPE).map(_.name).distinct
  }
}

object SparqlJinjaTemplate {

  private[templating] final val INPUT_CONFIG_SCOPE: Seq[String] = Seq("input", "config")
  private[templating] final val INPUT_ENTITY_SCOPE: Seq[String] = Seq("input", "entity")
  private[templating] final val OUTPUT_CONFIG_SCOPE: Seq[String] = Seq("output", "config")
}
