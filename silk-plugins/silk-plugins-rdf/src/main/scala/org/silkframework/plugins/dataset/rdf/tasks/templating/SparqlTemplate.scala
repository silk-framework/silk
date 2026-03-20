package org.silkframework.plugins.dataset.rdf.tasks.templating

import org.apache.jena.update.UpdateFactory
import org.silkframework.entity.EntitySchema
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.execution.local.EmptyEntityTable
import org.silkframework.runtime.templating.{CompiledTemplate, TemplateVariableName}
import org.silkframework.runtime.validation.ValidationException

import java.io.StringWriter
import scala.util.{Failure, Success, Try}

/**
  * Wraps a [[CompiledTemplate]] and adds SPARQL Update specific capabilities.
  */
class SparqlTemplate(template: CompiledTemplate) {

  /** Renders the template based on the variable assignments. */
  def generate(placeholderAssignments: Map[String, String], taskProperties: TaskProperties): String = {
    val values = scala.collection.mutable.LinkedHashMap[String, AnyRef]()
    // Flat entity values (used by simple template engine)
    placeholderAssignments.foreach { case (k, v) => values(k) = v }
    // SPARQL context objects (used by Velocity engine)
    values(SparqlVelocityTemplating.ROW_VAR_NAME) = Row(placeholderAssignments)
    values(SparqlVelocityTemplating.INPUT_PROPERTIES_VAR_NAME) = InputProperties(taskProperties.inputTask)
    values(SparqlVelocityTemplating.OUTPUT_PROPERTIES_VAR_NAME) = OutputProperties(taskProperties.outputTask)
    val writer = new StringWriter()
    template.evaluate(values.toMap, writer)
    writer.toString
  }

  /** Validates the template, including batch validation if batchSize > 1. */
  def validate(batchSize: Int): Unit = {
    template match {
      case compiled: VelocityCompiledTemplate
        if SparqlVelocityTemplating.templatingVariables.exists(varName =>
          compiled.variableMethodUsages(varName).exists(_.rowMethod == "rawUnsafe")) =>
        // We cannot generate meaningful example values for the template if $row.rawUnsafe() is used, because it could generate arbitrary SPARQL syntax.
      case _ =>
        // Generate example input assignments
        val genericUri = "urn:generic:1"
        val entityVariables = entityVariableNames
        val assignments = entityVariables.map(_ -> genericUri).toMap
        val inputPropVars = taskPropertyVariableNames("inputProperties").map(_ -> genericUri).toMap
        val outputPropVars = taskPropertyVariableNames("outputProperties").map(_ -> genericUri).toMap
        val taskProps = TaskProperties(inputPropVars, outputPropVars)
        val sparqlQuery = Try(generate(assignments, taskProps)) match {
          case Failure(exception) =>
            throw new ValidationException(
              "The SPARQL Update template could not be rendered with example values. Error message: " + exception.getMessage, exception)
          case Success(value) => value
        }
        Try(UpdateFactory.create(sparqlQuery)).failed.toOption.foreach { parseError =>
          throw new ValidationException(
            "The SPARQL Update template does not generate valid SPARQL Update queries. Error message: " +
              parseError.getMessage + ", example query: " + sparqlQuery)
        }
        if (batchSize > 1) {
          val batchSparql = sparqlQuery + "\n" + sparqlQuery
          Try(UpdateFactory.create(batchSparql)).failed.toOption.foreach { parseError =>
            throw new ValidationException(
              "The SPARQL Update template cannot be batched processed. There is probably a ';' missing at the end. Error message: " +
                parseError.getMessage + ", example batch query: " + batchSparql)
          }
        }
    }
  }

  /** The input entity schema that is expected by the template. */
  def inputSchema: EntitySchema = {
    val properties = entityVariableNames
    if (properties.isEmpty) {
      EmptyEntityTable.schema
    } else {
      EntitySchema("", properties.map(p => UntypedPath(p).asUntypedValueType).toIndexedSeq)
    }
  }

  /** True if the given template is static, i.e. contains no placeholder variables. */
  def isStaticTemplate: Boolean = {
    sparqlVariables match {
      case Some(vars) => vars.isEmpty
      case None => false
    }
  }

  /** Returns SPARQL-specific variables, extracting paths from method usages for Velocity templates. */
  private lazy val sparqlVariables: Option[Seq[TemplateVariableName]] = template match {
    case compiled: VelocityCompiledTemplate =>
      val rowVars = compiled.variableMethodUsages(SparqlVelocityTemplating.ROW_VAR_NAME)
        .map(u => new TemplateVariableName(u.parameterValue, ""))
      val inputPropVars = compiled.variableMethodUsages(SparqlVelocityTemplating.INPUT_PROPERTIES_VAR_NAME)
        .map(u => new TemplateVariableName(u.parameterValue, "inputProperties"))
      val outputPropVars = compiled.variableMethodUsages(SparqlVelocityTemplating.OUTPUT_PROPERTIES_VAR_NAME)
        .map(u => new TemplateVariableName(u.parameterValue, "outputProperties"))
      Some((rowVars ++ inputPropVars ++ outputPropVars).distinct)
    case _ => template.variables
  }

  /** Returns entity variable names (those with empty scope). */
  private def entityVariableNames: Seq[String] = {
    sparqlVariables match {
      case Some(vars) =>
        vars.filter(_.scope.isEmpty).map(_.name).distinct
      case None =>
        Seq.empty
    }
  }

  /** Returns variable names for a specific scope (e.g. "inputProperties", "outputProperties"). */
  private def taskPropertyVariableNames(scope: String): Seq[String] = {
    sparqlVariables match {
      case Some(vars) =>
        vars.filter(_.scope == scope).map(_.name).distinct
      case None =>
        Seq.empty
    }
  }
}

/** Makes properties of the input and output task of a SPARQL Update operator execution available. */
case class TaskProperties(inputTask: Map[String, String], outputTask: Map[String, String])
