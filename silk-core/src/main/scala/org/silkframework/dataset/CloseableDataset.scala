package org.silkframework.dataset

import org.silkframework.runtime.activity.UserContext
import scala.util.control.NonFatal

/**
  * Similar to Java's Closeable, but with user context.
  */
trait CloseableDataset {
  def close()(implicit userContext: UserContext): Unit
}

object CloseableDataset {

  /**
    * Automatically closes a CloseableDataset instance.
    *
    * Example:
    * {{{
    * using(createDataset) { dataset =>
    *   dataset.writeData(...)
    * }
    * }}}
    *
    */
  def using[DatasetType <: CloseableDataset, ReturnType](create: => DatasetType)
                                                        (f: DatasetType => ReturnType)
                                                        (implicit userContext: UserContext): ReturnType = {
    val dataset: DatasetType = create
    var exception: Throwable = null
    try {
      f(dataset)
    } catch {
      case NonFatal(e) =>
        exception = e
        throw e
    } finally {
      if (exception != null) {
        // We need to make sure that if close() is going to throw an exception,
        // it's neither overwriting the original exception nor getting swallowed.
        try {
          dataset.close()
        } catch {
          case NonFatal(closeException) =>
            exception.addSuppressed(closeException)
        }
      } else {
        dataset.close()
      }
    }
  }
}
