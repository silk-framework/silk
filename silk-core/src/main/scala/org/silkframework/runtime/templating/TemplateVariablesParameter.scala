package org.silkframework.runtime.templating

import org.silkframework.runtime.plugin.PluginObjectParameterNoSchema
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}
import org.silkframework.runtime.templating.TemplateVariables.TemplateVariablesFormat

import scala.xml.Node

/**
  * A parameter type that holds template variables, e.g., the execution-variable overrides of a workflow run.
  */
case class TemplateVariablesParameter(variables: TemplateVariables = TemplateVariables.empty) extends PluginObjectParameterNoSchema

object TemplateVariablesParameter {

  def empty: TemplateVariablesParameter = TemplateVariablesParameter()

  implicit object TemplateVariablesParameterXmlFormat extends XmlFormat[TemplateVariablesParameter] {

    override def read(value: Node)(implicit readContext: ReadContext): TemplateVariablesParameter = {
      TemplateVariablesParameter(TemplateVariablesFormat.read(value))
    }

    override def write(value: TemplateVariablesParameter)(implicit writeContext: WriteContext[Node]): Node = {
      TemplateVariablesFormat.write(value.variables)
    }
  }
}
