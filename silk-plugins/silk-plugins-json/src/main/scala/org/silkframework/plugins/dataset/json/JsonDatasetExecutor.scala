package org.silkframework.plugins.dataset.json

import org.silkframework.dataset.{EntitySink, LinkSink, TableLinkSink}
import org.silkframework.execution.local.LocalResourceBasedDatasetExecutor
import org.silkframework.runtime.activity.UserContext

/**
  * Executor for [[JsonDataset]]. Reads via the bulk-aware data source and writes JSON.
  */
class JsonDatasetExecutor extends LocalResourceBasedDatasetExecutor[JsonDataset] {

  override protected def createEntitySink(plugin: JsonDataset)(implicit userContext: UserContext): EntitySink =
    new JsonSink(plugin.bulkWritableResource, plugin.jsonTemplate, plugin.maxDepth)

  override protected def createLinkSink(plugin: JsonDataset)(implicit userContext: UserContext): LinkSink =
    new TableLinkSink(new JsonSink(plugin.bulkWritableResource, maxDepth = plugin.maxDepth))
}
