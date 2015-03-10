package models

import de.fuberlin.wiwiss.silk.runtime.activity.Observable

class TaskData[T](initialValue: T) extends Observable[T] {

  private var value = initialValue

  def apply() = value

  def update(newValue: T) {
    value = newValue
    publish(newValue)
  }
}
