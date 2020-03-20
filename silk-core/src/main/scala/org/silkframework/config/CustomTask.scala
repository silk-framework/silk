package org.silkframework.config

import org.silkframework.runtime.plugin.{AnyPlugin, PluginFactory, PluginRegistry}
import org.silkframework.runtime.resource.{ResourceLoader, ResourceManager}
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat, XmlSerialization}

import scala.xml.Node
/**
  * A custom task provided by a plugin.
  */
trait CustomTask extends TaskSpec with AnyPlugin {

  /** Retrieves a list of properties as key-value pairs for this task to be displayed to the user. */
  override def properties(implicit prefixes: Prefixes, resourceLoader: ResourceLoader): Seq[(String, String)] = {
    val (pluginType, params) = PluginRegistry.reflect(this)
    ("Type", pluginType.label) +: params.toSeq
  }

  override def withProperties(updatedProperties: Map[String, String])(implicit prefixes: Prefixes, resourceManager: ResourceManager): CustomTask = {
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

      implicit val prefixes = readContext.prefixes
      implicit val resources = readContext.resources
      val taskPlugin = PluginRegistry.create[CustomTask](pluginType, params)

      taskPlugin
    }

    def write(value: CustomTask)(implicit writeContext: WriteContext[Node]): Node = {
      val (pluginType, params) = PluginRegistry.reflect(value)(writeContext.prefixes, writeContext.resourceLoader)

      <CustomTask type={pluginType.id.toString}>{
        {XmlSerialization.serializeParameter(params)}
      }</CustomTask>
    }
  }


}
