package org.silkframework.runtime.templating

import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}
import org.silkframework.runtime.templating.TemplateVariable.TemplateVariableFormat
import org.silkframework.runtime.validation.BadUserInputException

import java.io.StringWriter
import java.util
import scala.collection.JavaConverters.mapAsJavaMapConverter
import scala.collection.mutable
import scala.xml.Node

/**
  * Holds a set of variables that can be used in parameter value templates.
  */
case class TemplateVariables(variables: Seq[TemplateVariable]) {

  val map: Map[String, TemplateVariable] = variables.map(v => (v.name, v)).toMap

  validate()

  /**
    * Returns variables as a map to be used in template evaluation.
    * The key is the scope and the values are all variables for this scope as a Java map.
    */
  def variableMap: Map[String, util.Map[String, String]] = {
    map.groupBy(_._2.scope).mapValues(_.mapValues(_.value).asJava)
  }

  /**
    * Lists all available scoped variable names.
    */
  def variableNames: Seq[String] = {
    for (variable <- variables.sortBy(_.name)) yield {
      variable.scopedName
    }
  }

  def resolved(additionalVariables: TemplateVariables = TemplateVariables.empty): TemplateVariables = {
    val resolvedVariables = mutable.Buffer[TemplateVariable]()
    for(variable <- variables) {
      variable.template match {
        case Some(template) =>
          val value = TemplateVariables(resolvedVariables).resolveTemplateValue(template, additionalVariables)
          resolvedVariables.append(variable.copy(value = value))
        case None =>
          resolvedVariables.append(variable)
      }
    }
    TemplateVariables(resolvedVariables)
  }

  /**
    * Resolves a template string.
    *
    * @throws TemplateEvaluationException If the template evaluation failed.
    * */
  def resolveTemplateValue(template: String, additionalVariables: TemplateVariables = TemplateVariables.empty): String = {
    val writer = new StringWriter()
    GlobalTemplateVariablesConfig.templateEngine().compile(template).evaluate(additionalVariables.map ++ variableMap, writer)
    writer.toString
  }

  /**
    * Merges this variables with another set of variables.
    */
  def merge(other: TemplateVariables): TemplateVariables = {
    TemplateVariables(variables ++ other.variables)
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
