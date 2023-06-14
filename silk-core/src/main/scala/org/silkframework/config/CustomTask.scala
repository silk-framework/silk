package org.silkframework.config

import org.silkframework.runtime.plugin.annotations.PluginType
import org.silkframework.runtime.plugin.{AnyPlugin, PluginContext, PluginFactory, PluginRegistry}
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat, XmlSerialization}
import org.silkframework.workspace.{OriginalTaskData, TaskLoadingException}

import scala.util.control.NonFatal
import scala.xml.Node

/**
  * A custom task provided by a plugin.
  */
@PluginType()
trait CustomTask extends TaskSpec with AnyPlugin {

  /** Retrieves a list of properties as key-value pairs for this task to be displayed to the user. */
  override def properties(implicit pluginContext: PluginContext): Seq[(String, String)] = {
    ("Type", pluginSpec.label) +: parameters.toStringMap.toSeq
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
