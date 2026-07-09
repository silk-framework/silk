package org.silkframework.plugins.dataset.csv

import org.silkframework.dataset.{EntitySink, LinkSink}
import org.silkframework.execution.local.LocalResourceBasedDatasetExecutor
import org.silkframework.runtime.activity.UserContext

/**
  * Executor for [[CsvDataset]]. Reads via the bulk-aware data source and writes CSV.
  */
class CsvDatasetExecutor extends LocalResourceBasedDatasetExecutor[CsvDataset] {

  override protected def createEntitySink(plugin: CsvDataset)(implicit userContext: UserContext): EntitySink =
    new CsvEntitySink(plugin.bulkWritableResource, plugin.csvSettings)

  override protected def createLinkSink(plugin: CsvDataset)(implicit userContext: UserContext): LinkSink =
    new CsvLinkSink(plugin.bulkWritableResource, plugin.csvSettings)
}
