package org.silkframework.plugins.dataset.text

import org.silkframework.dataset.{EntitySink, LinkSink}
import org.silkframework.execution.local.LocalResourceBasedDatasetExecutor
import org.silkframework.runtime.activity.UserContext

/**
  * Executor for [[TextFileDataset]]. Reads via the bulk-aware data source and writes plain text.
  */
class TextFileDatasetExecutor extends LocalResourceBasedDatasetExecutor[TextFileDataset] {

  override protected def createEntitySink(plugin: TextFileDataset)(implicit userContext: UserContext): EntitySink =
    new TextFileSink(plugin)

  override protected def createLinkSink(plugin: TextFileDataset)(implicit userContext: UserContext): LinkSink =
    new TextFileSink(plugin)
}
