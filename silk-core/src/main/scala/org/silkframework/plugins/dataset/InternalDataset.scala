package org.silkframework.plugins.dataset

import org.silkframework.dataset.{DataSource, Dataset, EntitySink, LinkSink}
import org.silkframework.runtime.plugin.{Plugin, PluginRegistry}

@Plugin(
  id = "internal",
  label = "Internal",
  description =
"""Dataset for storing entities between workflow steps."""
)
case class InternalDataset() extends Dataset {

  override def source: DataSource = InternalDataset.default().source

  override def linkSink: LinkSink = InternalDataset.default().linkSink

  override def entitySink: EntitySink = InternalDataset.default().entitySink

  override def clear() = InternalDataset.default().clear()
}

/**
  * Holds the default internal endpoint.
  * At the moment, the default can only be set programmatically and not in the configuration.
  */
object InternalDataset {

  @volatile
  private var datasetPlugin: Option[Dataset] = None

  def isAvailable: Boolean = datasetPlugin.nonEmpty

  def default(): Dataset = {
    if(datasetPlugin.isEmpty) {
      datasetPlugin = PluginRegistry.createFromConfigOption[Dataset]("dataset.internal")
      if(datasetPlugin.isEmpty)
        throw new IllegalAccessException("No internal dataset plugin has been configured at 'dataset.internal'.")
    }
    datasetPlugin.get
  }

}