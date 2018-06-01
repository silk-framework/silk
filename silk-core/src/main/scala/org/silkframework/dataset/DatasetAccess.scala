package org.silkframework.dataset

/**
  * Provides read and write access to a dataset.
  */
trait DatasetAccess extends DatasetReadAccess with DatasetWriteAccess

/**
  * Provides read access to a dataset.
  */
trait DatasetReadAccess {

  /**
    * Returns a data source for reading entities from the data set.
    */
  def source: DataSource

}

/**
  * Provides write access to a dataset.
  */
trait DatasetWriteAccess {

  /**
    * Returns a link sink for writing entity links to the data set.
    */
  def linkSink: LinkSink

  /**
    * Returns a entity sink for writing entities to the data set.
    */
  def entitySink: EntitySink

}
