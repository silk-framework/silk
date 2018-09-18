package org.silkframework.dataset

import org.silkframework.runtime.activity.UserContext

/**
  * Similar to Java's Closeable, but with user context.
  */
trait CloseableDataset {
  def close()(implicit userContext: UserContext): Unit
}
