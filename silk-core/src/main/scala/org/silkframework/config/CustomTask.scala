package org.silkframework.config

import org.silkframework.runtime.plugin.annotations.PluginType
import org.silkframework.runtime.plugin.{AnyPlugin, PluginFactory, PluginRegistry}
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat, XmlSerialization}
import org.silkframework.workspace.{OriginalTaskData, TaskLoadingException}

import scala.xml.Node

/**
  * A custom task provided by a plugin.
  */
@PluginType(
  label = "Workflow operator",
  description = "An operator that can be used in a workflow."
)
trait CustomTask extends TaskSpec with AnyPlugin

object CustomTask extends PluginFactory[CustomTask] {

  /**
    * XML serialization format.
    */
  implicit object CustomTaskFormat extends XmlFormat[CustomTask] {

    override def tagNames: Set[String] = Set("CustomTask")

    def read(node: Node)(implicit readContext: ReadContext): CustomTask = {
      val pluginType = (node \ "@type").text
      val taskPlugin = TaskLoadingException.withTaskLoadingException(OriginalTaskData(pluginType, XmlSerialization.deserializeParameters(node))) { params =>
        PluginRegistry.create[CustomTask](pluginType, params)
      }

      taskPlugin
    }

    def write(value: CustomTask)(implicit writeContext: WriteContext[Node]): Node = {
      <CustomTask type={value.pluginSpec.id.toString}>
        {XmlSerialization.serializeParameters(value.parameters)}
      </CustomTask>
    }
  }


}
