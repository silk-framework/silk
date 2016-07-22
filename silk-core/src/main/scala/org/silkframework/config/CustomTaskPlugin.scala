package org.silkframework.config

import org.silkframework.runtime.plugin.{AnyPlugin, PluginFactory, PluginRegistry}
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}

import scala.xml.Node

/**
  * A custom task provided by a plugin.
  */
trait CustomTaskPlugin extends TaskSpecification with AnyPlugin

object CustomTaskPlugin extends PluginFactory[CustomTaskPlugin] {

  /**
    * XML serialization format.
    */
  implicit object CustomTaskFormat extends XmlFormat[CustomTaskPlugin] {

    def read(node: Node)(implicit readContext: ReadContext): CustomTaskPlugin = {
      val pluginType = (node \ "@type").text
      val params = (node \ "Param" map (p => ((p \ "@name").text, (p \ "@value").text))).toMap

      implicit val prefixes = readContext.prefixes
      implicit val resources = readContext.resources
      val taskPlugin = PluginRegistry.create[CustomTaskPlugin](pluginType, params)

      taskPlugin
    }

    def write(value: CustomTaskPlugin)(implicit writeContext: WriteContext[Node]): Node = {
      val (pluginType, params) = PluginRegistry.reflect(value)

      <CustomTask type={pluginType.id.toString}>{
        for ((name, value) <- params) yield {
          <Param name={name} value={value}/>
        }
      }</CustomTask>
    }
  }


}
