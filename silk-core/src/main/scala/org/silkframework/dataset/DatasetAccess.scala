package org.silkframework.dataset

import org.silkframework.runtime.activity.UserContext

/**
  * Provides read and write access to a dataset.
  */
trait DatasetAccess extends DatasetReadAccess with DatasetWriteAccess

/**
  * Provides read access to a dataset.
  */
trait DatasetReadAccess {

  /**
    * Creates a new data source for reading entities from the data set.
    */
  def source(implicit userContext: UserContext): DataSource

}

/**
  * Provides write access to a dataset.
  */
trait DatasetWriteAccess {

  /**
    * Creates a new link sink for writing entity links to the data set.
    */
  def linkSink(implicit userContext: UserContext): LinkSink

  /**
    * Creates a new entity sink for writing entities to the data set.
    */
  def entitySink(implicit userContext: UserContext): EntitySink

}
