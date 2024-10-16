package org.silkframework.dataset.rdf

import org.silkframework.entity.Entity
import org.silkframework.runtime.iterator.CloseableIterator

/**
  * Provides an Iterator interface for [[Quad]] containing serialization and [[Entity]] transformation
  */
trait QuadIterator extends CloseableIterator[Quad] {
  /**
    * Should close and clean up all used resources. This will be called at most once.
    */
  protected def closeResources(): Unit

  private var isClosed = false

  /** Idempotent close */
  override def close(): Unit = synchronized {
    if(!isClosed) {
      isClosed = true
      closeResources()
    }
  }
}
