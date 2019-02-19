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