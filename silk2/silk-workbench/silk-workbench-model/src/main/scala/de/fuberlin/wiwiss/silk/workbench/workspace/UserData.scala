package de.fuberlin.wiwiss.silk.workbench.workspace

import collection.mutable.{Subscriber, Publisher, WeakHashMap}
import de.fuberlin.wiwiss.silk.util.Observable
import de.fuberlin.wiwiss.silk.util.task.{Status, Task, HasStatus, ValueTask}

/**
 * Holds user specific data.
 */
class UserData[T](initialValue: T) extends Observable[T] {
  /** Holds the current values of all users. */
  private val values = new WeakHashMap[User, T]()

  /**
   * Retrieves the current value.
   */
  def apply(): T = {
    values.get(User()) match {
      case Some(value) => value
      case None => initialValue
    }
  }

  /**
   * Updates the current value.
   */
  def update(newValue: T) {
    values.update(User(), newValue)
    publish(newValue)
  }
}

/**
 * Listens to the current value of the current users task.
 */
abstract class CurrentValueListener[T](userData: UserData[_ <: ValueTask[T]]) {

  userData.onUpdate(Listener)

  protected def onUpdate(value: T)

  private object Listener extends (ValueTask[T] => Unit) {
    def apply(task: ValueTask[T]) {
      task.value.onUpdate(ValueListener)
    }
  }

  private object ValueListener extends (T => Unit) {
    def apply(value: T) {
      onUpdate(value)
    }
  }
}

/**
 * Listens to the current status of the current users task.
 */
abstract class CurrentStatusListener[T](userData: UserData[_ <: HasStatus]) {

  userData.onUpdate(Listener)

  protected def onUpdate(value: Status)

  private object Listener extends (HasStatus => Unit) {
    def apply(task: HasStatus) {
      task.onUpdate(StatusListener)
    }
  }

  private object StatusListener extends (Status => Unit) {
    def apply(value: Status) {
      onUpdate(value)
    }
  }
}