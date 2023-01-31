package org.silkframework.config

import org.silkframework.runtime.plugin.{AnyPlugin, PluginContext, PluginFactory, PluginRegistry}
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat, XmlSerialization}

import scala.xml.Node
/**
  * A custom task provided by a plugin.
  */
trait CustomTask extends TaskSpec with AnyPlugin {

  /** Retrieves a list of properties as key-value pairs for this task to be displayed to the user. */
  override def properties(implicit prefixes: Prefixes): Seq[(String, String)] = {
    ("Type", pluginSpec.label) +: parameters.toSeq
  }

  override def withProperties(updatedProperties: Map[String, String])(implicit context: PluginContext): CustomTask = {
    withParameters(updatedProperties)
  }

}

object CustomTask extends PluginFactory[CustomTask] {

  /**
    * XML serialization format.
    */
  implicit object CustomTaskFormat extends XmlFormat[CustomTask] {

    override def tagNames: Set[String] = Set("CustomTask")

    def read(node: Node)(implicit readContext: ReadContext): CustomTask = {
      val pluginType = (node \ "@type").text
      val params = XmlSerialization.deserializeParameters(node)
      val templates = XmlSerialization.deserializeTemplates(node)
      val taskPlugin = PluginRegistry.create[CustomTask](pluginType, params, templates)

      taskPlugin
    }

    def write(value: CustomTask)(implicit writeContext: WriteContext[Node]): Node = {
      <CustomTask type={value.pluginSpec.id.toString}>
        {XmlSerialization.serializeParameters(value.parameters)}
        {XmlSerialization.serializeTemplates(value.templateValues)}
      </CustomTask>
    }
  }


}
