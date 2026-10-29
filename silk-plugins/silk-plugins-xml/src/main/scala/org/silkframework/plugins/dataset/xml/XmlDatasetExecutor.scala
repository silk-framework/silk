package org.silkframework.plugins.dataset.xml

import org.silkframework.dataset.{EntitySink, LinkSink, TableLinkSink}
import org.silkframework.execution.local.LocalResourceBasedDatasetExecutor
import org.silkframework.runtime.activity.UserContext

/**
  * Executor for [[XmlDataset]]. Reads via the bulk-aware data source and writes XML.
  */
class XmlDatasetExecutor extends LocalResourceBasedDatasetExecutor[XmlDataset] {

  override protected def createEntitySink(plugin: XmlDataset)(implicit userContext: UserContext): EntitySink =
    new XmlSink(plugin.bulkWritableResource, plugin.parsedOutputTemplate, plugin.maxDepth)

  override protected def createLinkSink(plugin: XmlDataset)(implicit userContext: UserContext): LinkSink =
    new TableLinkSink(new XmlSink(plugin.bulkWritableResource, plugin.parsedOutputTemplate, plugin.maxDepth))
}
