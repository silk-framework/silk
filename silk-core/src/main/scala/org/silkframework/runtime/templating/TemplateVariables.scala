package org.silkframework.runtime.templating

import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}
import org.silkframework.runtime.templating.TemplateVariable.TemplateVariableFormat

import java.io.StringWriter
import java.util
import scala.collection.JavaConverters.mapAsJavaMapConverter
import scala.xml.{Node, PCData}

/**
  * Holds a set of variables that can be used in parameter value templates.
  */
//TODO need to retain order
case class TemplateVariables(map: Map[String, TemplateVariable]) {

  def variables: Seq[TemplateVariable] = map.values.toSeq

  /**
    * Returns variables as a map to be used in template evaluation.
    * The key is the scope and the values are all variables for this scope as a Java map.
    */
  def variableMap: Map[String, util.Map[String, String]] = {
    map.groupBy(_._2.scope).mapValues(_.mapValues(_.value).asJava)
  }

  /**
    * Lists all available variable names.
    */
  def variableNames: Seq[String] = {
    for (variable <- map.values.toSeq.sortBy(_.name)) yield {
      variable.scope + "." + variable.name
    }
  }

  /**
    * Resolves a template string.
    *
    * @throws TemplateEvaluationException If the template evaluation failed.
    * */
  def resolveTemplateValue(value: String): String = {
    val writer = new StringWriter()
    GlobalTemplateVariablesConfig.templateEngine().compile(value).evaluate(variableMap, writer)
    writer.toString
  }

}

/**
  * A single template variable.
  */
case class TemplateVariable(name: String, value: String, scope: String, isSensitive: Boolean)

object TemplateVariable {

  /**
    * XML serialization format.
    */
  implicit object TemplateVariableFormat extends XmlFormat[TemplateVariable] {

    val tagName = "Variable"

    override def tagNames: Set[String] = Set(tagName)

    override def read(value: Node)(implicit readContext: ReadContext): TemplateVariable = {
      TemplateVariable(
        name = (value \ "@name").text,
        value = value.text,
        scope = (value \ "@scope").text,
        isSensitive = (value \ "@sensitive").text.toBoolean,
      )
    }

    override def write(value: TemplateVariable)(implicit writeContext: WriteContext[Node]): Node = {
      <Variable name={value.name} scope={value.scope} sensitive={value.isSensitive.toString} xml:space="preserve">{PCData(value.value)}</Variable>
    }
  }

}

object TemplateVariables {

  def empty: TemplateVariables = TemplateVariables(Map.empty)

  def fromVariables(variables: Seq[TemplateVariable]): TemplateVariables = {
    TemplateVariables(variables.map(v => (v.name, v)).toMap)
  }

  /**
    * XML serialization format.
    */
  implicit object TemplateVariablesFormat extends XmlFormat[TemplateVariables] {

    override def tagNames: Set[String] = Set("Variables")

    override def read(value: Node)(implicit readContext: ReadContext): TemplateVariables = {
      val variables = (value \ TemplateVariableFormat.tagName).map(TemplateVariableFormat.read)
      TemplateVariables.fromVariables(variables)
    }

    override def write(value: TemplateVariables)(implicit writeContext: WriteContext[Node]): Node = {
      <Variables>
      { value.variables.map(TemplateVariableFormat.write) }
      </Variables>
    }
  }

}
