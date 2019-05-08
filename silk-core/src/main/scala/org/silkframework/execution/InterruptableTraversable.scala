package org.silkframework.execution

/**
  * A wrapper for Traversables that will check for Thread.interrupted and throws an [[InterruptedException]].
  */
class InterruptableTraversable[T](traversable: Traversable[T]) extends Traversable[T] {
  override def foreach[U](f: T => U): Unit = {
    for(item <- traversable) {
      if(Thread.interrupted()) {
        throw new InterruptedException()
      }
      f(item)
    }
  }
}
