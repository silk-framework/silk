package org.silkframework.config

import org.silkframework.runtime.plugin.PluginRegistry
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}
import org.silkframework.util.Identifier
import scala.xml.Node

/**
  * A custom task specification provided by a plugin.
  */
case class CustomTaskSpecification(id: Identifier, plugin: CustomTaskPlugin)

object CustomTaskSpecification {

  /**
    * XML serialization format.
    */
  implicit object CustomTaskSpecificationFormat extends XmlFormat[CustomTaskSpecification] {

    def read(node: Node)(implicit readContext: ReadContext): CustomTaskSpecification = {
      implicit val prefixes = readContext.prefixes
      implicit val resources = readContext.resources

      val id = (node \ "@id").text
      val pluginType = (node \ "@type").text
      val params = (node \ "Param" map (p => ((p \ "@name").text, (p \ "@value").text))).toMap
      val taskPlugin = PluginRegistry.create[CustomTaskPlugin](pluginType, params)

      CustomTaskSpecification(id, taskPlugin)
    }

    def write(value: CustomTaskSpecification)(implicit writeContext: WriteContext[Node]): Node = {
      val (pluginType, params) = PluginRegistry.reflect(value.plugin)

      <CustomTask id={value.id} type={pluginType.id.toString}>{
        for ((name, value) <- params) yield {
            <Param name={name} value={value}/>
        }
      }</CustomTask>
    }
  }

}
