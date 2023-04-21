package org.silkframework.runtime.templating

import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}
import org.silkframework.runtime.templating.TemplateVariable.TemplateVariableFormat

import java.io.StringWriter
import java.util
import scala.collection.JavaConverters.mapAsJavaMapConverter
import scala.collection.mutable
import scala.xml.{Node, PCData}

/**
  * Holds a set of variables that can be used in parameter value templates.
  */
case class TemplateVariables(variables: Seq[TemplateVariable]) {

  val map: Map[String, TemplateVariable] = variables.map(v => (v.name, v)).toMap

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
    for (variable <- variables.sortBy(_.name)) yield {
      variable.scope + "." + variable.name
    }
  }

  def resolve(): TemplateVariables = {
    val resolvedVariables = mutable.Buffer[TemplateVariable]()
    for(variable <- variables) {
      variable.template match {
        case Some(template) =>
          val value = TemplateVariables(resolvedVariables).resolveTemplateValue(template)
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
  def resolveTemplateValue(template: String): String = {
    val writer = new StringWriter()
    GlobalTemplateVariablesConfig.templateEngine().compile(template).evaluate(variableMap, writer)
    writer.toString
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


/**
  * A single template variable.
  */
case class TemplateVariable(name: String, value: String, template: Option[String], isSensitive: Boolean, scope: String)

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
        value =(value \ "value").text,
        template = Option((value \ "template").text).filter(_.trim.nonEmpty),
        isSensitive = (value \ "@isSensitive").text.toBoolean,
        scope = (value \ "@scope").text,
      )
    }

    override def write(value: TemplateVariable)(implicit writeContext: WriteContext[Node]): Node = {
      <Variable name={value.name}
                isSensitive={value.isSensitive.toString}
                scope={value.scope}>
        <Value xml:space="preserve">{PCData(value.value)}</Value>
        { value.template.toSeq.map(template => <Template xml:space="preserve">{PCData(template)}</Template>) }
      </Variable>
    }
  }

}
