package org.silkframework.plugins.dataset.rdf

import org.silkframework.dataset.{DataSink, DataSource, DatasetPlugin}
import org.silkframework.runtime.plugin.{Plugin, PluginRegistry}

@Plugin(
  id = "internal",
  label = "Internal",
  description =
"""Dataset for storing entities between workflow steps."""
)
case class InternalDataset() extends DatasetPlugin {

  override def source: DataSource = InternalDataset.default().source

  override def sink: DataSink = InternalDataset.default().sink
}

/**
  * Holds the default Corporate Memory endpoint.
  * At the moment, the default can only be set programmatically and not in the configuration.
  */
object InternalDataset {

  @volatile
  private var datasetPlugin: Option[DatasetPlugin] = None

  def isAvailable: Boolean = datasetPlugin.nonEmpty

  def default(): DatasetPlugin = {
    if(datasetPlugin.isEmpty) {
      PluginRegistry.createFromConfigOption("dataset.internal") match {
        case Some(p) => datasetPlugin = p
        case None => throw new IllegalAccessException("No dataset plugin has been defined.")
      }
    }
    datasetPlugin.get
  }

  def setDefault(plugin: DatasetPlugin): Unit = {
    datasetPlugin = Some(plugin)
  }

}