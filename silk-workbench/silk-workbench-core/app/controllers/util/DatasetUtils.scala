package controllers.util

import org.silkframework.dataset.{Dataset, ResourceBasedDataset}
import org.silkframework.runtime.plugin.PluginRegistry

object DatasetUtils {
  /** Get the plugin IDs of all resource based datasets. */
  def resourceBasedDatasetPluginIds: Seq[String] = {
    PluginRegistry.availablePlugins[Dataset]
      .filter(plugin => classOf[ResourceBasedDataset].isAssignableFrom(plugin.pluginClass))
      .map(_.id.toString)
  }
}
