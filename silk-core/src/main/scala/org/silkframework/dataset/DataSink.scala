package org.silkframework.dataset

import java.io.Closeable

/**
 * Represents an abstraction over a data sink.
 */
trait DataSink extends Closeable {

  /**
    * Makes sure that the next write will start from an empty dataset.
    */
  def clear(): Unit

}
