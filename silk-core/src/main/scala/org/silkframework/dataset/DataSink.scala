package org.silkframework.dataset

import org.silkframework.runtime.activity.UserContext

/**
 * Represents an abstraction over a data sink.
 */
trait DataSink extends CloseableDataset {

  /**
    * Makes sure that the next write will start from an empty dataset.
    */
  def clear()(implicit userContext: UserContext): Unit
}
