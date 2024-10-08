package org.silkframework.dataset

import org.silkframework.runtime.activity.UserContext

/**
 * Represents an abstraction over a data sink.
 */
trait DataSink extends CloseableDataset {

  /**
    * Makes sure that the next write will start from an empty dataset.
    *
    * @param force If set to true, it should clear the dataset no matter what the config is.
    */
  def clear(force: Boolean = false)(implicit userContext: UserContext): Unit
}
