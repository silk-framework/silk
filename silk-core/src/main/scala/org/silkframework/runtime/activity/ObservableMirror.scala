package org.silkframework.runtime.activity

/**
  * An observable that mirrors the value of an underlying observable.
  */
class ObservableMirror[T](initialObservable: Observable[T]) extends Observable[T] {

  @volatile
  private var observable: Observable[T] = initialObservable

  private val subscriber: T => Unit = {
    value => publish(value)
  }

  override def isDefined: Boolean = {
    observable.isDefined
  }

  override def apply(): T = {
    observable.apply()
  }

  def updateObservable(newObservable: Observable[T]): Unit = {
    newObservable.subscribe(subscriber)
    observable.removeSubscription(subscriber)
    observable = newObservable
  }
}
