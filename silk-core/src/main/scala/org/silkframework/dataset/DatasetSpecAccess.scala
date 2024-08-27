package org.silkframework.dataset

import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.runtime.activity.UserContext

/**
  * Wrapper around a DatasetAccess instance that adds the DatasetSpec behaviour (URI column, readOnly)
  */
case class DatasetSpecAccess(datasetSpec: GenericDatasetSpec, datasetAccess: DatasetAccess) extends DatasetAccess {
  /**
   * Creates a new link sink for writing entity links to the data set.
   */
  override def linkSink(implicit userContext: UserContext): LinkSink = DatasetSpec.LinkSinkWrapper(datasetAccess.linkSink, datasetSpec)

  /**
   * Creates a new entity sink for writing entities to the data set.
   */
  override def entitySink(implicit userContext: UserContext): EntitySink  = DatasetSpec.EntitySinkWrapper(datasetAccess.entitySink, datasetSpec)

  /**
   * Creates a new data source for reading entities from the data set.
   */
  override def source(implicit userContext: UserContext): DataSource  = DatasetSpec.DataSourceWrapper(datasetAccess.source, datasetSpec)
}