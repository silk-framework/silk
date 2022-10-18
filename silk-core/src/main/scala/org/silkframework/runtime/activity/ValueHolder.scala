package org.silkframework.runtime.activity

/**
 * Holds the current value of an activity.
 */
class ValueHolder[T](initialValue: => Option[T]) extends Observable[T] {

  @volatile
  private var value: T = _

  @volatile
  private var initialized: Boolean = false

  override def isDefined: Boolean = {
    initialized || initialValue.isDefined
  }

  override def apply(): T = {
    if(!initialized) {
      if(initialValue.isDefined) {
        value = initialValue.get
        initialized = true
      } else {
        throw new NoSuchElementException(s"No value has been set and the activity does not define an initial value.")
      }
    }
    value
  }

  def update(v: T): Unit = {
    value = v
    initialized = true
    publish(v)
  }

  /** Re-publishes the current value. This is only used to force updates of subscribers even though the value has not changed. */
  def republish(): Unit = {
    publish(value)
  }

  /**
    * Updates the value by calling a provided update function.
    * @param func Function to be called with the current value. The value will be updated to the result of the function.
    */
  def updateWith(func: T => T): Unit = {
    update(func(apply()))
  }
}