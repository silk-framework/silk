package org.silkframework.util

import org.silkframework.runtime.resource.DoSomethingOnGC

import java.io.Closeable

/**
  * An iterator that must be closed after finishing consuming it.
  *
  * Implementers must close the data source when `hasNext` returns `false`.
  */
trait CloseableIterator[+T] extends Iterator[T] with Closeable { self =>

  def head: T = {
    headOption.getOrElse(throw new NoSuchElementException())
  }

  def headOption: Option[T] = {
    if(hasNext) {
      val element = next()
      close()
      Some(element)
    } else {
      None
    }
  }

  override def take(n: Int): CloseableIterator[T] = CloseableIterator(super.take(n), this)

  override def map[U](f: T => U): CloseableIterator[U] = {
    new MappedCloseableIterator(this, f)
  }

  override def filter(p: T => Boolean): CloseableIterator[T] = CloseableIterator(super.filter(p), this)

  override def flatMap[B](f: T => IterableOnce[B]): CloseableIterator[B] = {
    CloseableIterator(super.flatMap(f), this)
  }

  final def use[R](f: (Iterator[T] => R)): R =
    try f(this) finally close()

  final def use[R](f: => R): R =
    try f finally close()

  /**
    * Return a new CloseableIterator which also closes the supplied Closeable
    * object when itself gets closed.
    */
  final def thenClose(c: Closeable): CloseableIterator[T] = new CloseableIterator[T] {
    def hasNext = self.hasNext
    def next() = self.next()
    def close() = try self.close() finally c.close()
  }
}

object CloseableIterator {

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

  def apply[T](iterator: Iterator[T]): CloseableIterator[T] = {
    new WrappedCloseableIterator[T](iterator)
  }

  def apply[T](iterable: IterableOnce[T]): CloseableIterator[T] = {
    new WrappedCloseableIterator[T](iterable.iterator)
  }

}

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

private class WrappedCloseableIterator[+T](iterator: Iterator[T]) extends CloseableIterator[T] {

  override def hasNext: Boolean = iterator.hasNext

  override def next(): T = iterator.next()

  override def close(): Unit = {
    // nothing to close
  }
}

private class MappedCloseableIterator[+T, +U](iterator: CloseableIterator[T], f: T => U) extends CloseableIterator[U] {

  def hasNext: Boolean = iterator.hasNext

  def next(): U = f(iterator.next())

  def close(): Unit = iterator.close()

}