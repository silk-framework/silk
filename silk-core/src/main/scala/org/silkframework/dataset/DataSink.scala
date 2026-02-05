package org.silkframework.dataset

import org.silkframework.runtime.activity.UserContext

/**
 * Represents an abstraction over a data sink.
 */
trait DataSink extends CloseableDataset {

  /** Makes sure that the next write will start from an empty dataset.
    *
    * @param force Forces the clearing of the dataset. E.g. even when "clear before execution" flag is not set.
    */
  def clear(force: Boolean = false)(implicit userContext: UserContext): Unit
}
