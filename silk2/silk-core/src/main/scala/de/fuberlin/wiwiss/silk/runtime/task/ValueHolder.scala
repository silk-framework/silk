package de.fuberlin.wiwiss.silk.runtime.task

import de.fuberlin.wiwiss.silk.util.Observable

/**
 * Holds an observable value.
 */
class ValueHolder[T](initialValue: T) extends Observable[T] {

  @volatile private var currentValue = initialValue

  def apply(): T = currentValue

  def update(v: T) {
    currentValue = v
    publish(v)
  }
}