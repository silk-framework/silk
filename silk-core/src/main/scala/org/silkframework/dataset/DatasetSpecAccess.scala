package org.silkframework.dataset

import org.silkframework.config.ProductionConfig
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.runtime.activity.UserContext

/**
  * Wrapper around a DatasetAccess instance that adds the DatasetSpec behaviour (URI column, readOnly, safe-mode).
  */
case class DatasetSpecAccess(datasetSpec: GenericDatasetSpec, datasetAccess: DatasetAccess) extends DatasetAccess {
  /**
   * Creates a new data source for reading entities from the data set.
   */
  override def source(implicit userContext: UserContext): DataSource = {
    DatasetSpecAccess.safeAccess(DatasetSpec.DataSourceWrapper(datasetAccess.source, datasetSpec), SafeModeDataSource, datasetSpec)
  }

  /**
   * Creates a new entity sink for writing entities to the data set.
   */
  override def entitySink(implicit userContext: UserContext): EntitySink = {
    DatasetSpecAccess.safeAccess(DatasetSpec.EntitySinkWrapper(datasetAccess.entitySink, datasetSpec), SafeModeSink, datasetSpec)
  }

  /**
   * Creates a new link sink for writing entity links to the data set.
   */
  override def linkSink(implicit userContext: UserContext): LinkSink = {
    DatasetSpec.checkDatasetAllowsWriteAccess(None, datasetSpec.readOnly)
    DatasetSpecAccess.safeAccess(DatasetSpec.LinkSinkWrapper(datasetAccess.linkSink, datasetSpec), SafeModeSink, datasetSpec)
  }
}

object DatasetSpecAccess {

  /** True if access should be prevented regarding the dataset and safe-mode config. */
  def preventAccessInSafeMode(datasetSpec: GenericDatasetSpec)(implicit userContext: UserContext): Boolean = {
    ProductionConfig.inSafeMode && !datasetSpec.plugin.isFileResourceBased && !userContext.executionContext.insideWorkflow
  }

  /** Create a data access object or return the safe-mode fallback. */
  def safeAccess[T](create: T, fallback: T, datasetSpec: GenericDatasetSpec)
                   (implicit userContext: UserContext): T = {
    if (preventAccessInSafeMode(datasetSpec)) {
      fallback
    } else {
      create
    }
  }
}
