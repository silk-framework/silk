package org.silkframework.dataset.rdf

/**
  * An iterator that must be closed after finishing consuming it.
  */
trait ClosableIterator[T] extends Iterator[T] {
  /** This must be called if the iterator will not be consumed any further.
    * This function must be idem-potent.
    * */
  def close(): Unit
}

object ClosableIterator {
  /**
    * Creates a closable iterator from an existing closable iterator.
    * All returned values are automatically mapped.
    */
  def mappedClosableIterator[T, U](closableIterator: ClosableIterator[T],
                                   f: T => U): ClosableIterator[U] = new ClosableIterator[U] {
    override def close(): Unit = closableIterator.close()

    override def hasNext: Boolean = closableIterator.hasNext

    override def next(): U = f(closableIterator.next())
  }
}
