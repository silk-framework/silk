package org.silkframework.plugins.dataset.rdf.tasks.templating

import org.apache.jena.update.UpdateFactory
import org.silkframework.entity.EntitySchema
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.execution.local.EmptyEntityTable
import org.silkframework.plugins.dataset.rdf.tasks.templating.SparqlUpdateTemplate.{InputProperties, OutputProperties, Row}
import org.silkframework.runtime.templating.{CompiledTemplate, TemplateEngines, TemplateMethodUsage, TemplateVariableName}
import org.silkframework.runtime.validation.ValidationException

import java.io.StringWriter
import scala.util.{Failure, Success, Try}

/**
  * Wraps a [[CompiledTemplate]] and adds SPARQL Update specific capabilities.
  */
class SparqlUpdateTemplate(template: CompiledTemplate) {

  /**
   * Renders the template based on the variable assignments.
   *
   * @param placeholderAssignments For each placeholder in the query template.
   * @param taskProperties The input and output task properties.
   * */
  def generate(placeholderAssignments: Map[String, String], taskProperties: TaskProperties): String = {
    val values = scala.collection.mutable.LinkedHashMap[String, AnyRef]()
    // Flat entity values (used by simple template engine)
    placeholderAssignments.foreach { case (k, v) => values(k) = v }
    // SPARQL context objects (used by Velocity engine)
    values(SparqlUpdateTemplate.ROW_VAR_NAME) = Row(placeholderAssignments)
    values(SparqlUpdateTemplate.INPUT_PROPERTIES_VAR_NAME) = InputProperties(taskProperties.inputTask)
    values(SparqlUpdateTemplate.OUTPUT_PROPERTIES_VAR_NAME) = OutputProperties(taskProperties.outputTask)
    val writer = new StringWriter()
    template.evaluate(values.toMap, writer)
    writer.toString
  }

  /** Validates the template, including batch validation if batchSize > 1. */
  def validate(batchSize: Int): Unit = {
    if (usesRawUnsafe) {
      // We cannot generate meaningful example values for the template if $row.rawUnsafe() is used, because it could generate arbitrary SPARQL syntax.
    } else {
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

  /** SPARQL-specific method names that accept a string parameter representing an input path. */
  private val sparqlMethodNames = Set("uri", "plainLiteral", "rawUnsafe", "exists")

  /** Returns SPARQL-specific variables, extracting paths from method usages. */
  private lazy val sparqlVariables: Option[Seq[TemplateVariableName]] = {
    val usages = SparqlUpdateTemplate.templatingVariables.flatMap(v => template.methodUsages(v))
    if (usages.nonEmpty) {
      val rowVars = sparqlMethodUsages(SparqlUpdateTemplate.ROW_VAR_NAME)
        .map(u => new TemplateVariableName(u.parameterValue, ""))
      val inputPropVars = sparqlMethodUsages(SparqlUpdateTemplate.INPUT_PROPERTIES_VAR_NAME)
        .map(u => new TemplateVariableName(u.parameterValue, "inputProperties"))
      val outputPropVars = sparqlMethodUsages(SparqlUpdateTemplate.OUTPUT_PROPERTIES_VAR_NAME)
        .map(u => new TemplateVariableName(u.parameterValue, "outputProperties"))
      Some((rowVars ++ inputPropVars ++ outputPropVars).distinct)
    } else {
      template.variables
    }
  }

  /** Returns method usages on the given variable filtered to SPARQL-specific methods. */
  private def sparqlMethodUsages(variableName: String): Seq[TemplateMethodUsage] = {
    template.methodUsages(variableName).filter(u => sparqlMethodNames.contains(u.methodName))
  }

  /** Checks if any SPARQL templating variable uses the rawUnsafe method. */
  private lazy val usesRawUnsafe: Boolean = {
    SparqlUpdateTemplate.templatingVariables.exists(varName =>
      sparqlMethodUsages(varName).exists(_.methodName == "rawUnsafe"))
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

object SparqlUpdateTemplate {

  private final val ROW_VAR_NAME = "row"
  private final val INPUT_PROPERTIES_VAR_NAME = "inputProperties"
  private final val OUTPUT_PROPERTIES_VAR_NAME = "outputProperties"

  private final val templatingVariables = Seq(ROW_VAR_NAME, INPUT_PROPERTIES_VAR_NAME, OUTPUT_PROPERTIES_VAR_NAME)

  /**
   * Creates a SPARQL template from a string.
   */
  def create(templateEngineId: String, template: String, batchSize: Int): SparqlUpdateTemplate = {
    val templateEngine = TemplateEngines.create(templateEngineId)
    val sparqlTemplate = new SparqlUpdateTemplate(templateEngine.compile(template))
    sparqlTemplate.validate(batchSize)
    sparqlTemplate
  }

  /** Row API used in SPARQL templates. Represents a single row where input paths are either exactly one value or empty.
   *
   * The Row object will be available in Velocity templates as 'row' variable.
   *
   * Examples:
   *
   * <pre>
   *   $row.uri("urn:prop:uriProp") ## Renders the value of the input path as URI, e.g. <http://...>
   *   $row.plainLiteral("urn:prop:stringProp") ## Renders the value of the input paths as plain string, e.g. "Quotes \" are escaped"
   *   $row.rawUnsafe("urn:prop:trustedValuesOnly") ## Puts the value as it is into the rendered template. This is UNSAFE and prone to injection attacks.
   *   #if ( $row.exists("urn:prop:valueMightNotExist") ) ## Checks if a value exists for the input path, i.e. values can always be optional.
   *     $row.plainLiteral("urn:prop:valueMightNotExist") ## If no value exists for the input path then this would throw an exception
   *   #end
   * </pre>
   *
   * @param inputValues The map of existing input values, i.e. values that were defined by input paths, but where no value was available are not set.
   */
  case class Row(inputValues: Map[String, String]) extends TemplateValueAccessApi {
    override val templateVarName: String = ROW_VAR_NAME
  }

  /** Similar to Row, but for the input task properties. */
  case class InputProperties(inputValues: Map[String, String]) extends TemplateValueAccessApi {
    override val templateVarName: String = INPUT_PROPERTIES_VAR_NAME
  }

  /** Similar to Row, but for the output task properties. */
  case class OutputProperties(inputValues: Map[String, String]) extends TemplateValueAccessApi {
    override val templateVarName: String = OUTPUT_PROPERTIES_VAR_NAME
  }

}

/** Makes properties of the input and output task of a SPARQL Update operator execution available. */
case class TaskProperties(inputTask: Map[String, String], outputTask: Map[String, String])
