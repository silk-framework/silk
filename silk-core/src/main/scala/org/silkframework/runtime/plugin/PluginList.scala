package org.silkframework.runtime.plugin

import scala.collection.immutable.ListMap

/**
  * A list of plugins grouped by type.
  */
case class PluginList(pluginsByType: ListMap[String, Seq[PluginDescription[_]]])

object PluginList {
  def load(pluginTypes: Seq[String]): PluginList= {
    val pluginsByType =
      for(pluginType <- pluginTypes) yield {
        val pluginClass = getClass.getClassLoader.loadClass(pluginType)
        (pluginType, PluginRegistry.availablePluginsForClass(pluginClass))
      }

    PluginList(ListMap(pluginsByType: _*))
  }
}