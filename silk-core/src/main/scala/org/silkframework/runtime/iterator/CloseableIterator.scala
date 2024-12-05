package org.silkframework.runtime.iterator

import org.silkframework.runtime.resource.DoSomethingOnGC

import java.io.Closeable
import java.util.logging.Logger

/**
  * An iterator that must be closed after finishing consuming it.
  */
trait CloseableIterator[+T] extends Iterator[T] with Closeable {

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

  /**
    * Iterates all elements and closes the iterator.
    */
  override def foreach[U](f: T => U): Unit = {
    try {
      while (hasNext) {
        f(next())
      }
    } finally {
      close()
    }
  }

  /**
    * Selects the first ''n'' elements.
    * The returned iterable will also close the original iterator.
    */
  override def take(n: Int): CloseableIterator[T] = wrap(super.take(n))

  /**
    * Filter this iterator.
    * The returned iterator will also close the original iterator.
    */
  override def filter(p: T => Boolean): CloseableIterator[T] = wrap(super.filter(p))

  /**
    * Map this iterator.
    * The returned iterator will also close the original iterator.
    */
  override def map[U](f: T => U): CloseableIterator[U] = wrap(super.map(f))

  /**
    * Flatmap this iterator.
    * The returned iterator will also close the original iterator.
    */
  override def flatMap[B](f: T => IterableOnce[B]): CloseableIterator[B] = wrap(super.flatMap(f))

  /**
    * Returns a new closeable iterator that also closes the supplied Closeable.
    */
  final def thenClose(closeable: Closeable): CloseableIterator[T] = {
    new ChainedCloseableIterator(this, closeable)
  }

  /**
    * Use this iterator and close it afterwards.
    */
  final def use[R](f: (Iterator[T] => R)): R = {
    try {
      f(this)
    } finally {
      close()
    }
  }

  /**
    * Wraps a modified version of this iterator into a new closeable iterator.
    */
  protected def wrap[B](iterator: Iterator[B]): CloseableIterator[B] = {
    new CloseResourceIterator[B](iterator, this)
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
    * @param closeable Resource close after iteration.
    */
  def apply[T](iterator: Iterator[T], closeable: Closeable): CloseableIterator[T] = {
    new CloseResourceIterator[T](iterator, closeable) with AutoClose[T]
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

  /**
    * Creates a closeable iterator for a single element.
    */
  def single[T](element: T): CloseableIterator[T] = {
    new WrappedCloseableIterator[T](Iterator(element))
  }

}

/**
  *  Can be mixed into `CloseableIterator` implementations to call close automatically after iteration.
  *  Prints a warning if `close()` is never called (at garbage collection).
  */
trait AutoClose[+T] extends CloseableIterator[T] with DoSomethingOnGC {

  private val log: Logger = Logger.getLogger(getClass.getName)

  @volatile
  private var isClosed: Boolean = false

  abstract override def hasNext: Boolean = {
    if (super.hasNext) {
      true
    } else {
      close()
      false
    }
  }

  abstract override def next(): T = {
    super.next()
  }

  abstract override def close(): Unit = {
    if (!isClosed) {
      super.close()
      isClosed = true
    }
  }

  /**
    * Closes the iterator on Garbage Collection.
    * Code should not rely on this and close the iterator after usage.
    */
  override def finalAction(): Unit = {
    if (!isClosed) {
      log.warning(s"${iterator.getClass} has not been closed. Cleaning up resources.")
      close()
    }
  }
}

/**
  * Iterator that closes another resource after iteration.
  */
private class CloseResourceIterator[+T](iterator: Iterator[T], closeable: Closeable) extends CloseableIterator[T] {
  @volatile
  private var closed = false

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
    if(!closed) {
      closeable.close()
      closed = true
    }
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

/**
  * Closeable iterator that also closes another Closeable.
  */
private class ChainedCloseableIterator[+T](iterator: CloseableIterator[T], closeable: Closeable) extends CloseableIterator[T] {
  @volatile
  private var closed = false

  override def hasNext: Boolean = {
    if (iterator.hasNext) {
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
    if(!closed) {
      try {
        iterator.close()
      } finally {
        closeable.close()
        closed = true
      }
    }
  }
}
