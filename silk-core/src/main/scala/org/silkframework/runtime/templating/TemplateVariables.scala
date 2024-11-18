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
  def resolveTemplateValue(template: String): String = {
    val writer = new StringWriter()
    GlobalTemplateVariablesConfig.templateEngine().compile(template).evaluate(variables, writer)
    writer.toString
  }

  /**
    * Merges this variables with another set of variables.
    */
  def merge(other: TemplateVariables): TemplateVariables = {
    TemplateVariables(variables ++ other.variables)
  }

  /**
   * Returns a copy with an added variable at the beginning.
   */
  def withFirst(variable: TemplateVariable): TemplateVariables = {
    TemplateVariables(variable +: variables)
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
