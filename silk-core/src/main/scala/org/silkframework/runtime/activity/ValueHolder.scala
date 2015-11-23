package org.silkframework.runtime.activity

/**
 * Holds the current value of an activity.
 */
class ValueHolder[T](initialValue: => Option[T]) extends Observable[T] {

  @volatile
  private var value: T = _
  for(v <- initialValue)
    value = v

  override def apply(): T = value

  def update(v: T) {
    value = v
    publish(v)
  }
}