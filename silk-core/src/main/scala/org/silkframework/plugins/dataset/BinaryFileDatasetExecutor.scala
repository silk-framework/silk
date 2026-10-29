package org.silkframework.plugins.dataset

import org.silkframework.dataset.{EntitySink, LinkSink}
import org.silkframework.execution.local.LocalResourceBasedDatasetExecutor
import org.silkframework.runtime.activity.UserContext

/**
  * Executor for [[BinaryFileDataset]]. Reads file entities via the bulk-aware data source and writes
  * binary files; generic entities and links are not supported.
  */
class BinaryFileDatasetExecutor extends LocalResourceBasedDatasetExecutor[BinaryFileDataset] {

  override protected def createEntitySink(plugin: BinaryFileDataset)(implicit userContext: UserContext): EntitySink =
    new FileSink(plugin.file)

  override protected def createLinkSink(plugin: BinaryFileDataset)(implicit userContext: UserContext): LinkSink =
    throw new RuntimeException("Only file entities can be written to this dataset. Links are not supported")
}
