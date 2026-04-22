package org.silkframework.plugins.dataset.rdf.tasks.templating

import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.execution.local.EmptyEntityTable
import org.silkframework.plugins.dataset.rdf.tasks.templating.SparqlLegacyTemplate._
import org.silkframework.runtime.templating.{CompiledTemplate, TemplateMethodUsage, TemplateVariableName, TemplateVariableValue}
import org.silkframework.runtime.validation.ValidationException

import java.io.StringWriter
import scala.util.{Failure, Success, Try}

/**
  * SPARQL template implementation for the Velocity and Simple template engines.
  *
  * Exposes input entity values via a `row` object, and the connected input/output task parameters via
  * `inputProperties` / `outputProperties` objects. All three objects offer the methods defined on
  * [[TemplateValueAccessApi]] (`uri`, `plainLiteral`, `rawUnsafe`, `exists`).
  */
class SparqlLegacyTemplate(template: CompiledTemplate) extends SparqlTemplate {

  override def generate(entity: Option[Entity],
                        taskProperties: TaskProperties,
                        templateVariables: Seq[TemplateVariableValue] = Seq.empty): Iterable[String] = {
    entity match {
      case Some(e) if e.values.nonEmpty =>
        val properties = e.schema.typedPaths.map(_.normalizedSerialization)
        CrossProductIterator(e.values, properties).map(renderOnce(_, taskProperties)).toSeq
      case _ =>
        Seq(renderOnce(Map.empty, taskProperties))
    }
  }

  private def renderOnce(placeholderAssignments: Map[String, String],
                         taskProperties: TaskProperties): String = {
    val values = scala.collection.mutable.LinkedHashMap[String, AnyRef]()
    // Flat entity values (used by simple template engine)
    placeholderAssignments.foreach { case (k, v) => values(k) = v }
    // SPARQL context objects
    values(ROW_VAR_NAME) = Row(placeholderAssignments)
    values(INPUT_PROPERTIES_VAR_NAME) = InputProperties(taskProperties.inputTask)
    values(OUTPUT_PROPERTIES_VAR_NAME) = OutputProperties(taskProperties.outputTask)
    val writer = new StringWriter()
    template.evaluate(values.toMap, writer)
    writer.toString
  }

  override def generateWithDefaults(): String = {
    val genericUri = "urn:generic:1"
    val entityVariables = entityVariableNames
    val assignments = entityVariables.map(_ -> genericUri).toMap
    val inputPropVars = taskPropertyVariableNames(Seq(INPUT_PROPERTIES_VAR_NAME)).map(_ -> genericUri).toMap
    val outputPropVars = taskPropertyVariableNames(Seq(OUTPUT_PROPERTIES_VAR_NAME)).map(_ -> genericUri).toMap
    val taskProps = TaskProperties(inputPropVars, outputPropVars)
    Try(renderOnce(assignments, taskProps)) match {
      case Failure(exception) =>
        throw new ValidationException(
          "The SPARQL Update template could not be rendered with example values. Error message: " + exception.getMessage, exception)
      case Success(value) => value
    }
  }

  override def validateUpdateQuery(batchSize: Int): Unit = {
    if (!usesRawUnsafe) {
      // Skipped for rawUnsafe templates: they can generate arbitrary SPARQL syntax so example-query validation is unreliable.
      SparqlTemplate.validateParseability(generateWithDefaults(), batchSize)
    }
  }

  override def inputSchema: EntitySchema = {
    val properties = entityVariableNames
    if (properties.isEmpty) {
      EmptyEntityTable.schema
    } else {
      EntitySchema("", properties.map(p => UntypedPath(p).asUntypedValueType).toIndexedSeq)
    }
  }

  override def isStaticTemplate: Boolean = {
    sparqlVariables match {
      case Some(vars) => vars.isEmpty
      case None => false
    }
  }

  /** SPARQL-specific method names that accept a string parameter representing an input path. */
  private val sparqlMethodNames = Set("uri", "plainLiteral", "rawUnsafe", "exists")

  /** Returns SPARQL-specific variables, extracting paths from method usages. */
  private lazy val sparqlVariables: Option[Seq[TemplateVariableName]] = {
    val usages = templatingVariables.flatMap(v => template.methodUsages(v))
    if (usages.nonEmpty) {
      val rowVars = sparqlMethodUsages(ROW_VAR_NAME)
        .map(u => new TemplateVariableName(u.parameterValue))
      val inputPropVars = sparqlMethodUsages(INPUT_PROPERTIES_VAR_NAME)
        .map(u => new TemplateVariableName(u.parameterValue, Seq(INPUT_PROPERTIES_VAR_NAME)))
      val outputPropVars = sparqlMethodUsages(OUTPUT_PROPERTIES_VAR_NAME)
        .map(u => new TemplateVariableName(u.parameterValue, Seq(OUTPUT_PROPERTIES_VAR_NAME)))
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
    templatingVariables.exists(varName =>
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
  private def taskPropertyVariableNames(scope: Seq[String]): Seq[String] = {
    sparqlVariables match {
      case Some(vars) =>
        vars.filter(_.scope == scope).map(_.name).distinct
      case None =>
        Seq.empty
    }
  }
}

object SparqlLegacyTemplate {

  private[templating] final val ROW_VAR_NAME = "row"
  private[templating] final val INPUT_PROPERTIES_VAR_NAME = "inputProperties"
  private[templating] final val OUTPUT_PROPERTIES_VAR_NAME = "outputProperties"

  private final val templatingVariables = Seq(ROW_VAR_NAME, INPUT_PROPERTIES_VAR_NAME, OUTPUT_PROPERTIES_VAR_NAME)

  /** Row API. Represents a single row where input paths are either exactly one value or empty.
    *
    * Available in templates as the `row` variable.
    *
    * Examples (Velocity):
    *
    * <pre>
    *   $row.uri("urn:prop:uriProp") ## Renders the value as a URI, e.g. <http://...>
    *   $row.plainLiteral("urn:prop:stringProp") ## Renders the value as a plain literal, e.g. "Quotes \" are escaped"
    *   $row.rawUnsafe("urn:prop:trustedValuesOnly") ## Puts the value as-is; UNSAFE, prone to injection.
    *   #if ( $row.exists("urn:prop:valueMightNotExist") )
    *     $row.plainLiteral("urn:prop:valueMightNotExist")
    *   #end
    * </pre>
    *
    * @param inputValues Map of available input values. Paths without a value are absent.
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

  /**
    * Iterates over the cross-product of per-property value lists, producing one `Map[String, String]`
    * per combination. Used by the legacy template engine, which renders one query per combination.
    *
    * Preserves the existing behavior of emitting at least one (empty) assignment, even if all value
    * lists are empty.
    */
  private[templating] case class CrossProductIterator(values: IndexedSeq[Seq[String]],
                                                      properties: IndexedSeq[String]) extends Iterator[Map[String, String]] {
    assert(values.nonEmpty)
    private val sizes = values.map(_.size).toArray
    // Holds the current index combination
    private val indexes = new Array[Int](values.size)
    private val firstNonEmptyIdx = sizes.zipWithIndex.filter(_._1 > 0).map(_._2).headOption.getOrElse(-1) // -1 if all are empty
    private val lastIndex = values.size - 1
    private var first: Boolean = true // This makes sure that at least one assignment is always generated

    override def hasNext: Boolean = first || firstNonEmptyIdx > -1 && (indexes(firstNonEmptyIdx) < sizes(firstNonEmptyIdx))

    override def next(): Map[String, String] = {
      if (!hasNext) {
        throw new IllegalStateException("Iterator is fully consumed and has no more values!")
      }
      val nextAssignment = indexes.zipWithIndex.collect {
        case (valueIdx, propertyIndex) if sizes(propertyIndex) > 0 => properties(propertyIndex) -> values(propertyIndex)(valueIdx)
      }.toMap
      setNextIndexCombinations()
      first = false
      nextAssignment
    }

    private def setNextIndexCombinations(): Unit = {
      var idx = lastIndex
      while (idx > -1) {
        indexes(idx) += 1
        if (indexes(idx) >= sizes(idx) && idx != firstNonEmptyIdx) { // Do not reset the first index, because of hasNext check
          indexes(idx) = 0
          idx -= 1
        } else if (idx > 0) {
          for (i <- (idx + 1) to lastIndex) { // null all index values after this index
            indexes(i) = 0
          }
          idx = -1
        } else {
          idx = -1
        }
      }
    }
  }
}
