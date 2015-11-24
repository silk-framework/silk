package org.silkframework.runtime.activity

/**
 * Holds the current value of an activity.
 */
class ValueHolder[T](initialValue: => Option[T]) extends Observable[T] {

  @volatile
  private var value: T = _

  @volatile
  private var initialized: Boolean = false

  override def apply(): T = {
    if(!initialized && initialValue.isDefined) {
      value = initialValue.get
      initialized = true
    }
    value
  }

  def update(v: T) {
    value = v
    initialized = true
    publish(v)
  }
}