package de.fuberlin.wiwiss.silk.runtime.activity

/**
 * Holds the current value of an activity.
 */
class ValueHolder[T](initialValue: => T) extends Observable[T] {

  @volatile
  private var value: T = _
  try { value = initialValue } catch { case _: NotImplementedError => }

  override def apply(): T = value

  def update(v: T) {
    value = v
    publish(v)
  }
}