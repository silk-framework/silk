package org.silkframework.runtime.iterator

import scala.annotation.tailrec

/**
  * Combines multiple closeable iterators into a single closeable iterator.
  *
  * @param nextIterator Function that retrieves the next iterator, if any.
  */
class RepeatedIterator[T](nextIterator: () => Option[CloseableIterator[T]]) extends CloseableIterator[T] {

  private var currentIterator: Option[CloseableIterator[T]] = {
    nextIterator()
  }

  override def hasNext: Boolean = {
    currentIterator match {
      case Some(current) =>
        if(current.hasNext) {
          true
        } else {
          updateNextIterator()
        }
      case None =>
        false
    }
  }

  override def next(): T = {
    currentIterator match {
      case Some(current) =>
        current.next()
      case None =>
        updateNextIterator()
        currentIterator match {
          case Some(current) =>
            current.next()
          case None =>
            throw new NoSuchElementException("No more items")
        }
    }
  }

  override def close(): Unit = {
    for (current <- currentIterator) {
      current.close()
    }
  }

  /**
    * Retrieves the next iterator.
    *
    * @return True, if there are more elements. False, otherwise.
    */
  @tailrec
  private def updateNextIterator(): Boolean = {
    for(current <- currentIterator) {
      current.close()
    }
    currentIterator = nextIterator()
    currentIterator match {
      case Some(iterator) if iterator.hasNext =>
        true
      case Some(_) =>
        updateNextIterator()
      case None =>
        false
    }
  }
}

