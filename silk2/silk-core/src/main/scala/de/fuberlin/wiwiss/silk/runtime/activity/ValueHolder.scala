package de.fuberlin.wiwiss.silk.runtime.activity

/**
 * Holds the current value of an activity.
 */
class ValueHolder[T]() extends Observable[T] {

  @volatile
  private var value: Option[T] = None

  def get = value

  def hasValue = value.isDefined

  override def apply(): T = value.getOrElse(throw new NoSuchElementException("Tried to request a value from an activity which did not generate a value yet."))

  def update(v: T) {
    value = Some(v)
    publish(v)
  }
}