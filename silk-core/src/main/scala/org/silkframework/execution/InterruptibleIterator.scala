package org.silkframework.execution

import org.silkframework.runtime.iterator.CloseableIterator

/**
  * A wrapper for Traversables that will check for Thread.interrupted and throws an [[InterruptedException]].
  */
class InterruptibleIterator[T](iterator: CloseableIterator[T]) extends CloseableIterator[T] {

  override def hasNext: Boolean = iterator.hasNext

  override def next(): T = {
    if (Thread.interrupted()) {
      throw new InterruptedException()
    }
    iterator.next()
  }

  override def close(): Unit = {
    iterator.close()
  }
}
