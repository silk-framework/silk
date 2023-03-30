package org.silkframework.runtime.activity

/**
 * Holds the current value of an activity.
 */
class ValueHolder[T](initialValue: => Option[T]) extends Observable[T] {

  @volatile
  private var value: Option[T] = None

  override def isDefined: Boolean = {
    value.isDefined || initialValue.isDefined
  }

  override def apply(): T = {
    value match {
      case Some(v) =>
        v
      case None =>
        initialValue match {
          case Some(v) =>
            value = Some(v)
            v
          case None =>
            throw new NoSuchElementException(s"No value has been set and the activity does not define an initial value.")
        }
    }
  }

  def update(v: T): Unit = {
    value = Some(v)
    publish(v)
  }

  /** Re-publishes the current value. This is only used to force updates of subscribers even though the value has not changed. */
  def republish(): Unit = {
    for(v <- value) {
      publish(v)
    }
  }

  /**
    * Updates the value by calling a provided update function.
    * @param func Function to be called with the current value. The value will be updated to the result of the function.
    */
  def updateWith(func: T => T): Unit = {
    update(func(apply()))
  }
}