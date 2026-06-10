package org.silkframework.runtime.templating

import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}
import org.silkframework.runtime.templating.TemplateVariable.TemplateVariableFormat
import org.silkframework.runtime.templating.exceptions.{TemplateEvaluationException, TemplateVariableEvaluationException, TemplateVariablesEvaluationException}
import org.silkframework.runtime.validation.BadUserInputException

import java.io.StringWriter
import scala.collection.mutable
import scala.xml.Node

/**
  * Holds a set of variables that can be used in parameter value templates.
  */
case class TemplateVariables(variables: Seq[TemplateVariable]) {

  lazy val map: Map[String, TemplateVariable] = variables.map(v => (v.name, v)).toMap

  validate()

  /**
    * Lists all available scoped variable names.
    */
  def variableNames: Seq[String] = {
    for (variable <- variables.sortBy(_.name)) yield {
      variable.scopedName
    }
  }

  /**
    * Resolves all templates and fills the template values accordingly.
    *
    * @throws TemplateVariablesEvaluationException If at least one template variable could not be resolved.
    */
  def resolved(additionalVariables: TemplateVariables = TemplateVariables.empty): TemplateVariables = {
    val resolvedVariables = mutable.Buffer[TemplateVariable]()
    val errors = mutable.Buffer[TemplateVariableEvaluationException]()
    for(variable <- variables) {
      variable.template match {
        case Some(template) =>
          try {
            val value = TemplateVariables(additionalVariables.variables ++ resolvedVariables).resolveTemplateValue(template)
            resolvedVariables.append(variable.copy(value = value))
          } catch {
            case ex: TemplateEvaluationException =>
              errors.append(TemplateVariableEvaluationException(variable, ex))
          }
        case None =>
          resolvedVariables.append(variable)
      }
    }
    if(errors.isEmpty) {
      TemplateVariables(resolvedVariables.toSeq)
    } else {
      throw TemplateVariablesEvaluationException(errors.toSeq)
    }
  }

  /**
    * Resolves a template string.
    *
    * @throws TemplateEvaluationException If the template evaluation failed.
    * */
  def resolveTemplateValue(template: String, evaluationConfig: EvaluationConfig = EvaluationConfig()): String = {
    val writer = new StringWriter()
    GlobalTemplateVariablesConfig.templateEngine().compile(template).evaluate(variables, writer, evaluationConfig)
    writer.toString
  }

  /**
    * Merges this variables with another set of variables.
    */
  def merge(other: TemplateVariables): TemplateVariables = {
    TemplateVariables(variables ++ other.variables)
  }

  /**
    * The execution-scope fallback variables for this set: for every variable in the `task`, `project`
    * or `global` scope whose name is not already present in the `execution` scope, an `execution`-scoped
    * copy is produced, using the highest-precedence source (task > project > global). This lets
    * `{{execution.X}}` resolve to the task, project or global variable `X` when `X` has not been set
    * directly in the execution scope. Sensitivity and templates of the source variable are preserved.
    */
  def executionScopeFallback: Seq[TemplateVariable] = {
    val executionNames = variables.iterator.filter(_.scope == TemplateVariableScopes.execution).map(_.name).toSet

    // Precedence by exact scope: task > project > global (smaller wins). Other scopes do not fall back.
    def precedence(scope: Seq[String]): Option[Int] = scope match {
      case TemplateVariableScopes.task    => Some(0)
      case TemplateVariableScopes.project => Some(1)
      case TemplateVariableScopes.global  => Some(2)
      case _                              => None
    }

    variables
      .flatMap(v => precedence(v.scope).map(p => (v, p)))
      .filterNot { case (v, _) => executionNames.contains(v.name) }
      .groupBy { case (v, _) => v.name }
      .map { case (_, candidates) => candidates.minBy { case (_, p) => p }._1.copy(scope = TemplateVariableScopes.execution) }
      .toSeq
  }

  /**
    * Returns a copy of these variables augmented with [[executionScopeFallback]] entries, so that
    * `{{execution.X}}` resolves to the task, project or global variable `X` when `X` has not been set
    * directly in the execution scope.
    */
  def withExecutionScopeFallback: TemplateVariables = {
    TemplateVariables(variables ++ executionScopeFallback)
  }

  /**
   * Returns a copy with an added variable at the beginning.
   */
  def withFirst(variable: TemplateVariable): TemplateVariables = {
    TemplateVariables(variable +: variables)
  }

  /**
   * Returns a copy with an added variable at the end.
   */
  def withLast(variable: TemplateVariable): TemplateVariables = {
    TemplateVariables(variables :+ variable)
  }

  /**
   * Returns only non-sensitive variables
   */
  def withoutSensitiveVariables(): TemplateVariables = {
    TemplateVariables(variables.filterNot(_.isSensitive))
  }

  private def validate(): Unit = {
    val duplicateNames = variables.groupBy(_.scopedName).filter(_._2.size > 1).keys
    if (duplicateNames.nonEmpty) {
      throw new BadUserInputException("Duplicate variable names: " + duplicateNames.mkString(", "))
    }
  }

}

object TemplateVariables {

  def empty: TemplateVariables = TemplateVariables(Seq.empty)

  /**
    * XML serialization format.
    */
  implicit object TemplateVariablesFormat extends XmlFormat[TemplateVariables] {

    override def tagNames: Set[String] = Set("Variables")

    override def read(value: Node)(implicit readContext: ReadContext): TemplateVariables = {
      val variables = (value \ TemplateVariableFormat.tagName).map(TemplateVariableFormat.read)
      TemplateVariables(variables)
    }

    override def write(value: TemplateVariables)(implicit writeContext: WriteContext[Node]): Node = {
      <Variables>
        { value.variables.map(TemplateVariableFormat.write) }
      </Variables>
    }
  }

}
