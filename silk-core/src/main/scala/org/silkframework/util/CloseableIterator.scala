package org.silkframework.util

import org.silkframework.runtime.resource.DoSomethingOnGC

import java.io.Closeable

/**
  * An iterator that must be closed after finishing consuming it.
  */
trait CloseableIterator[+T] extends Iterator[T] with Closeable { self =>

  /**
    * Returns the first element and closes this iterator.
    */
  def head: T = {
    headOption.getOrElse(throw new NoSuchElementException())
  }

  /**
    * Returns the first element (if any) and closes this iterator.
    */
  def headOption: Option[T] = {
    try {
      if (hasNext) {
        Some(next())
      } else {
        None
      }
    } finally {
      close()
    }

  }

  override def take(n: Int): CloseableIterator[T] = wrap(super.take(n))

  override def map[U](f: T => U): CloseableIterator[U] = wrap(super.map(f))

  override def filter(p: T => Boolean): CloseableIterator[T] = wrap(super.filter(p))

  override def flatMap[B](f: T => IterableOnce[B]): CloseableIterator[B] = wrap(super.flatMap(f))

  /**
    * Returns a new closeable iterator that also closes the supplied Closeable.
    */
  final def thenClose(c: Closeable): CloseableIterator[T] = new CloseableIterator[T] {
    def hasNext: Boolean = self.hasNext
    def next(): T = self.next()
    def close(): Unit = {
      try {
        self.close()
      } finally {
        c.close()
      }
    }
  }

  /**
    * Wraps a modified version of this iterator into a new closeable iterator.
    */
  protected def wrap[B](iterator: Iterator[B]): CloseableIterator[B] = {
    new AutoCloseableIterator[B](iterator, this)
  }
}

object CloseableIterator {

  /**
    * Empty closeable iterator.
    */
  def empty: CloseableIterator[Nothing] = CloseableIterator(Iterator.empty)

  /**
    * Creates a iterator that closes another resource after iteration.
    *
    * @param iterator The iterator.
    * @param closeable Resource to be close after iteration.
    */
  def apply[T](iterator: Iterator[T], closeable: Closeable): CloseableIterator[T] = {
    new AutoCloseableIterator[T](iterator, closeable)
  }

  /**
    * Creates a closeable iterator from an ordinary iterator that does not require any cleanup on close.
    */
  def apply[T](iterator: Iterator[T]): CloseableIterator[T] = {
    new WrappedCloseableIterator[T](iterator)
  }

  /**
    * Creates a closeable iterator from an iterable that does not require any cleanup on close.
    */
  def apply[T](iterable: IterableOnce[T]): CloseableIterator[T] = {
    new WrappedCloseableIterator[T](iterable.iterator)
  }

}

/**
  * Iterator that closes another resource after iteration.
  */
private class AutoCloseableIterator[+T](iterator: Iterator[T], closeable: Closeable) extends CloseableIterator[T] with DoSomethingOnGC {

  override def hasNext: Boolean = {
    if(iterator.hasNext) {
      true
    } else {
      close()
      false
    }
  }

  override def next(): T = {
    iterator.next()
  }

  override def close(): Unit = {
    closeable.close()
  }

  override def finalAction(): Unit = {
    //TODO log warning?
    close()
  }
}

/**
  * Closeable iterator that does nothing on close.
  */
private class WrappedCloseableIterator[+T](iterator: Iterator[T]) extends CloseableIterator[T] {

  override def hasNext: Boolean = iterator.hasNext

  override def next(): T = iterator.next()

  override def close(): Unit = {
    // nothing to close
  }
}
