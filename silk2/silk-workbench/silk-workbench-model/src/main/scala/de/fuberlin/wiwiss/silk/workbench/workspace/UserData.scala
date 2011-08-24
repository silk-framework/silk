package de.fuberlin.wiwiss.silk.workbench.workspace

import collection.mutable.{Subscriber, Publisher, WeakHashMap}
import de.fuberlin.wiwiss.silk.util.Observable
import de.fuberlin.wiwiss.silk.util.task.{Status, Task, HasStatus, ValueTask}
import java.util.concurrent.{TimeUnit, Callable, Executors}
import de.fuberlin.wiwiss.silk.workbench.workspace.User.CurrentTaskChanged

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

trait Listener[T] extends Observable[T] {

  /** The minimum number of milliseconds between two successive calls to onUpdate. */
  var maxFrequency = 5000

  /** The time of the last call to onUpdate */
  @volatile private var lastUpdateTime = 0L

  /** Indicates if a call to onUpdate is scheduled */
  @volatile private var scheduled = false

  /** The last message */
  @volatile private var lastMessage: Option[T] = None

  protected def update(value: T) {
    if(scheduled) {
      lastMessage = Some(value)
    } else {
      val time = System.currentTimeMillis() - lastUpdateTime
      if (time > maxFrequency) {
        println("IMMIDIATE UPDATE")
        onUpdate(value)
        lastUpdateTime = System.currentTimeMillis()
      } else {
        scheduled = true
        lastMessage = Some(value)
        delayedUpdate(maxFrequency)
      }
    }
  }

  protected def onUpdate(value: T) {
    publish(value)
  }

  private def delayedUpdate(delay: Long) {
    Listener.executor.schedule(new Runnable {
      def run() {
        scheduled = false
        lastUpdateTime = System.currentTimeMillis()
        println("DELAYED UPDATE")
        onUpdate(lastMessage.get)
      }
    }, delay, TimeUnit.MILLISECONDS)
  }
}

object Listener {
  private val executor = Executors.newScheduledThreadPool(1)


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
abstract class CurrentStatusListener(userData: UserData[_ <: HasStatus]) extends Listener[Status] {

  userData.onUpdate(Listener)

  private object Listener extends (HasStatus => Unit) {
    def apply(task: HasStatus) {
      task.onUpdate(StatusListener)
    }
  }

  private object StatusListener extends (Status => Unit) {
    def apply(value: Status) {
      update(value)
    }
  }
}