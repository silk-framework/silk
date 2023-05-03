package org.silkframework.execution

/**
  * A wrapper for Traversables that will check for Thread.interrupted and throws an [[InterruptedException]].
  */
class InterruptibleIterable[T](iterable: Iterable[T]) extends Iterable[T] {

  override def iterator: Iterator[T] = new InterruptibleIterator(iterable.iterator)
}

class InterruptibleIterator[T](iterator: Iterator[T]) extends Iterator[T] {

  override def hasNext: Boolean = iterator.hasNext

  override def next(): T = {
    if (Thread.interrupted()) {
      throw new InterruptedException()
    }
    iterator.next()
  }
}
