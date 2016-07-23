package org.silkframework.config

import org.silkframework.runtime.plugin.{AnyPlugin, PluginFactory, PluginRegistry}
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}

import scala.xml.Node

/**
  * A custom task provided by a plugin.
  */
trait CustomTask extends TaskSpec with AnyPlugin

object CustomTask extends PluginFactory[CustomTask] {

  /**
    * XML serialization format.
    */
  implicit object CustomTaskFormat extends XmlFormat[CustomTask] {

    def read(node: Node)(implicit readContext: ReadContext): CustomTask = {
      val pluginType = (node \ "@type").text
      val params = (node \ "Param" map (p => ((p \ "@name").text, (p \ "@value").text))).toMap

      implicit val prefixes = readContext.prefixes
      implicit val resources = readContext.resources
      val taskPlugin = PluginRegistry.create[CustomTask](pluginType, params)

      taskPlugin
    }

    def write(value: CustomTask)(implicit writeContext: WriteContext[Node]): Node = {
      val (pluginType, params) = PluginRegistry.reflect(value)

      <CustomTask type={pluginType.id.toString}>{
        for ((name, value) <- params) yield {
          <Param name={name} value={value}/>
        }
      }</CustomTask>
    }
  }


}
