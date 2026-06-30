package org.silkframework.dataset

/**
 * An empty data set. Its data access (an empty source and no-op sinks) is provided by the
 * `EmptyDatasetExecutor`.
 */
object EmptyDataset extends Dataset with Serializable {

  /**
    * Clears the contents of this dataset.
    */
  def clear(): Unit = { }

  override def characteristics: DatasetCharacteristics = DatasetCharacteristics.attributesOnly()
}
