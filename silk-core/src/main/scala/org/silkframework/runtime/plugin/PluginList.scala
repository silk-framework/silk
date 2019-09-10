package org.silkframework.runtime.plugin

import scala.collection.immutable.ListMap

/**
  * A list of plugins grouped by type.
  *
  * @param serializeMarkdownDocumentation Flag to specify that the Markdown documentation should be added in the serialization, e.g. JSON.
  */
case class PluginList(pluginsByType: ListMap[String, Seq[PluginDescription[_]]],
                      serializeMarkdownDocumentation: Boolean)

object PluginList {
  def load(pluginTypes: Seq[String],
           withMarkdownDocumentation: Boolean): PluginList= {
    val pluginsByType =
      for(pluginType <- pluginTypes) yield {
        val pluginClass = getClass.getClassLoader.loadClass(pluginType)
        (pluginType, PluginRegistry.availablePluginsForClass(pluginClass))
      }

    // Plugins are always loaded with Markdown documentation, this flag is only relevant for the serialization
    PluginList(ListMap(pluginsByType: _*), serializeMarkdownDocumentation = withMarkdownDocumentation)
  }
}