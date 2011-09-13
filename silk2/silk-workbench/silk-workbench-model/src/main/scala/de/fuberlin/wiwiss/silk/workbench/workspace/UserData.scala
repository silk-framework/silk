package de.fuberlin.wiwiss.silk.workbench.workspace

import collection.mutable.{Subscriber, Publisher, WeakHashMap}
import de.fuberlin.wiwiss.silk.util.Observable
import de.fuberlin.wiwiss.silk.util.task.{TaskStatus, Task, HasStatus, ValueTask}
import java.util.concurrent.{TimeUnit, Callable, Executors}
import de.fuberlin.wiwiss.silk.workbench.workspace.User.CurrentTaskChanged
import java.util.logging.{Logger, Level}

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

//TODO write tests and move to its own file
trait Listener[T] extends Observable[T] {

  /** The minimum number of milliseconds between two successive calls to onUpdate. */
  var maxFrequency = 5000

  /** The time of the last call to onUpdate */
  @volatile private var lastUpdateTime = 0L

  /** Indicates if a call to onUpdate is scheduled */
  @volatile private var scheduled = false

  /** The last message */
  @volatile private var lastMessage: Option[T] = None

  private val logger = Logger.getLogger(getClass.getName)

  protected def update(value: T) {
    if(scheduled) {
      lastMessage = Some(value)
    } else {
      val time = System.currentTimeMillis() - lastUpdateTime
      if (time > maxFrequency) {
        //println("IMMIDIATE UPDATE")
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
        try {
          scheduled = false
          lastUpdateTime = System.currentTimeMillis()
          //println("DELAYED UPDATE")
          onUpdate(lastMessage.get)
        } catch {
          case ex: Exception => logger.log(Level.WARNING, "Error on update", ex)
        }
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
abstract class CurrentTaskValueListener[T](userData: UserData[_ <: ValueTask[T]]) {

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
class CurrentTaskStatusListener(userData: UserData[_ <: HasStatus]) extends Listener[TaskStatus] with HasStatus {

  updateStatus(userData().status)
  userData.onUpdate(Listener)
  statusLogLevel = Level.FINEST
  progressLogLevel = Level.FINEST

  @volatile private var task = userData()

  private object Listener extends (HasStatus => Unit) {
    def apply(task: HasStatus) {
      task.onUpdate(StatusListener)
    }
  }

  private object StatusListener extends (TaskStatus => Unit) {
    def apply(status: TaskStatus) {
      update(status)
      updateStatus(status)
    }
  }
}