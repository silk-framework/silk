package org.silkframework.execution.local

import org.silkframework.config.Task
import org.silkframework.dataset.bulk.BulkResourceBasedDataset
import org.silkframework.dataset.{DataSource, Dataset, DatasetAccess, DatasetSpec, DatasetSpecAccess, EntitySink, LinkSink}
import org.silkframework.runtime.activity.UserContext

/**
  * Base executor for resource (file) based datasets that may also be backed by a zip archive.
  *
  * The bulk-aware data source is built from the dataset's resource(s) (see
  * [[BulkResourceBasedDataset.createDataSource]]); subclasses only provide the entity and link sinks.
  * The result is wrapped with the DatasetSpec behaviour (URI attribute, read-only, safe-mode) via
  * [[DatasetSpecAccess]], so callers always go through the execution-scoped access.
  */
abstract class LocalResourceBasedDatasetExecutor[DatasetType <: Dataset with BulkResourceBasedDataset]
    extends LocalDatasetExecutor[DatasetType] {

  protected def createEntitySink(plugin: DatasetType)(implicit userContext: UserContext): EntitySink

  protected def createLinkSink(plugin: DatasetType)(implicit userContext: UserContext): LinkSink

  override def access(task: Task[DatasetSpec[DatasetType]], execution: LocalExecution): DatasetAccess = {
    val plugin = task.data.plugin
    val rawAccess = new DatasetAccess {
      override def source(implicit userContext: UserContext): DataSource = plugin.createDataSource
      override def entitySink(implicit userContext: UserContext): EntitySink = createEntitySink(plugin)
      override def linkSink(implicit userContext: UserContext): LinkSink = createLinkSink(plugin)
    }
    DatasetSpecAccess(task.data, rawAccess)
  }
}
