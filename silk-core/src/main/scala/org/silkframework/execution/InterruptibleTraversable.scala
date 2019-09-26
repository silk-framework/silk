package org.silkframework.execution

/**
  * A wrapper for Traversables that will check for Thread.interrupted and throws an [[InterruptedException]].
  */
class InterruptibleTraversable[T](traversable: Traversable[T]) extends Traversable[T] {
  override def foreach[U](f: T => U): Unit = {
    for(item <- traversable) {
      if(Thread.interrupted()) {
        throw new InterruptedException()
      }
      f(item)
    }
  }
}

/** Mix of interruptible and mapped traversable. */
class MappedInterruptibleTraversable[T, U](traversable: Traversable[T], mappingFn: T => U) extends Traversable[U] {
  override def foreach[V](f: U => V): Unit = {
    val interruptibleTraversable = new InterruptibleTraversable(traversable)
    interruptibleTraversable foreach { entry =>
      f(mappingFn(entry))
    }
  }
}

/** Wraps a traversable with a traversable that maps the entries of the wrapped traversables to other values. */
class MappedTraversable[T, U](traversable: Traversable[T], mappingFn: T => U) extends Traversable[U] {
  override def foreach[V](f: U => V): Unit = {
    traversable foreach { entry =>
      f(mappingFn(entry))
    }
  }
}

/** Wraps a traversable with a traversable that filters the entries of the wrapped traversables. */
class FilteredTraversable[T](traversable: Traversable[T], filterFn: T => Boolean) extends Traversable[T] {
  override def foreach[U](f: T => U): Unit = {
    traversable foreach { elem =>
      if(filterFn(elem)) {
        f(elem)
      }
    }
  }
}