package org.silkframework.runtime.templating

import org.silkframework.runtime.plugin.PluginObjectParameterNoSchema
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}
import org.silkframework.runtime.templating.TemplateVariables.TemplateVariablesFormat

import scala.xml.Node

/**
  * A parameter type that holds workflow variables.
  * Workflow variables are set at the beginning of each workflow execution and are available in templates.
  */
case class TemplateVariablesParameter(variables: TemplateVariables = TemplateVariables.empty) extends PluginObjectParameterNoSchema {

  /**
    * Merges these variables with overrides.
    * Override values replace defaults with the same name.
    */
  def merge(overrides: TemplateVariablesParameter): TemplateVariablesParameter = {
    TemplateVariablesParameter(variables merge overrides.variables)
  }
}

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
