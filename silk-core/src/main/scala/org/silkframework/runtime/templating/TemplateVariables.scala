package org.silkframework.runtime.templating

import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}
import org.silkframework.runtime.templating.TemplateVariable.TemplateVariableFormat
import org.silkframework.runtime.templating.exceptions.{SensitiveVariableReferenceException, TemplateEvaluationException, TemplateVariableEvaluationException, TemplateVariablesEvaluationException, UnboundVariablesException}
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
    * A template may reference the preceding variables of this set. Sensitive ones are only available to sensitive variables,
    * so that a sensitive value cannot leak into a variable that is not marked as sensitive.
    *
    * @param additionalVariables Variables of the parent scopes. Callers are expected to pass them without sensitive variables.
    * @throws TemplateVariablesEvaluationException If at least one template variable could not be resolved.
    */
  def resolved(additionalVariables: TemplateVariables = TemplateVariables.empty): TemplateVariables = {
    val resolvedVariables = mutable.Buffer[TemplateVariable]()
    val errors = mutable.Buffer[TemplateVariableEvaluationException]()
    for(variable <- variables) {
      variable.template match {
        case Some(template) =>
          try {
            resolvedVariables.append(variable.copy(value = resolveTemplate(variable, template, additionalVariables, resolvedVariables.toSeq)))
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
    * Resolves all templates like [[resolved]], but keeps the stored value of each variable whose template
    * cannot be resolved (e.g. a template referencing a sensitive parent variable, which is not available
    * for template resolution). Kept variables still participate in the resolution of subsequent variables
    * with their stored value.
    *
    * @param rejectSensitiveReferences Rejects a reference to a sensitive sibling instead of keeping the stored value.
    *                                  Set when saving user input; loading keeps values stored before the rule existed.
    * @throws TemplateVariablesEvaluationException If a sensitive sibling is referenced and rejectSensitiveReferences is set.
    */
  def resolvedKeepingUnresolved(additionalVariables: TemplateVariables = TemplateVariables.empty,
                                rejectSensitiveReferences: Boolean = false): TemplateVariables = {
    val resolvedVariables = mutable.Buffer[TemplateVariable]()
    for (variable <- variables) {
      variable.template match {
        case Some(template) =>
          try {
            resolvedVariables.append(variable.copy(value = resolveTemplate(variable, template, additionalVariables, resolvedVariables.toSeq)))
          } catch {
            case ex: SensitiveVariableReferenceException if rejectSensitiveReferences =>
              throw TemplateVariablesEvaluationException(Seq(TemplateVariableEvaluationException(variable, ex)))
            case _: TemplateEvaluationException =>
              resolvedVariables.append(variable) // Keep the stored value
          }
        case None =>
          resolvedVariables.append(variable)
      }
    }
    TemplateVariables(resolvedVariables.toSeq)
  }

  /**
    * Resolves the template of one of these variables against the parent variables and the preceding variables of this set.
    * A reference to a sensitive sibling from a variable that is not sensitive is rejected as such, not as an undefined variable.
    *
    * @throws TemplateEvaluationException If the template could not be evaluated.
    */
  def resolveTemplate(variable: TemplateVariable, template: String, additionalVariables: TemplateVariables, preceding: Seq[TemplateVariable]): String = {
    try {
      TemplateVariables(additionalVariables.variables ++ TemplateVariables.referenceable(variable, preceding)).resolveTemplateValue(template)
    } catch {
      case ex: UnboundVariablesException if !variable.isSensitive =>
        val sensitiveSiblings = ex.missingVars.filter(isSensitiveMember)
        if (sensitiveSiblings.nonEmpty) throw new SensitiveVariableReferenceException(sensitiveSiblings, Some(ex)) else throw ex
    }
  }

  private def isSensitiveMember(name: TemplateVariableName): Boolean = {
    map.get(name.name).exists(member => member.isSensitive && member.scope == name.scope)
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
    * The preceding variables of the same scope that a variable may reference in its template.
    * Sensitive variables are only available to sensitive variables, so that their values cannot leak into non-sensitive ones.
    */
  def referenceable(variable: TemplateVariable, preceding: Seq[TemplateVariable]): Seq[TemplateVariable] = {
    if (variable.isSensitive) preceding else preceding.filterNot(_.isSensitive)
  }

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
