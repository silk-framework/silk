package de.fuberlin.wiwiss.silk.workbench.workspace

import collection.mutable.WeakHashMap
import de.fuberlin.wiwiss.silk.util.Observable
import de.fuberlin.wiwiss.silk.util.task.{TaskStatus, HasStatus, ValueTask}
import java.util.concurrent.{TimeUnit, Executors}
import java.util.logging.{Logger, Level}
import de.fuberlin.wiwiss.silk.workbench.workspace.User.CurrentTaskChanged

/**
 * Holds temporary user data for the current task.
 *
 * @param initialValue The initial value for a new task.
 */
class TaskData[T](initialValue: T) extends Observable[T] {
  /** Holds the current values of all users. */
  private val values = new WeakHashMap[User, T]()

  /** Holds the listeners for all users. */
  private val listeners = new WeakHashMap[User, User.Message => Unit]()

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
    val user = User()
    //Update task value for this user
    values.update(user, newValue)
    //Reset value if the task has been changed
    val handler = new MessageHandler()
    user.onUpdate(handler)
    listeners.update(user, handler)
    //Publish new value to observers
    publish(newValue)
  }

  private class MessageHandler() extends (User.Message => Unit) {
    def apply(msg: User.Message) {
      msg match {
        case CurrentTaskChanged(user, None, task) =>
          values.update(user, initialValue)
        case CurrentTaskChanged(user, Some(previousTask), task) if previousTask.name != task.name =>
          values.update(user, initialValue)
        case _ =>
      }
    }
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

abstract class TaskDataListener[T](userData: TaskData[T]) extends Listener[T] {
  userData.onUpdate(Listener)

  private object Listener extends (T => Unit) {
    def apply(value: T) {
      update(value)
    }
  }
}

/**
 * Listens to the current value of the current users task.
 */
abstract class CurrentTaskValueListener[T](userData: TaskData[_ <: ValueTask[T]]) extends Listener[T] {
  userData.onUpdate(Listener)

  private object Listener extends (ValueTask[T] => Unit) {
    def apply(task: ValueTask[T]) {
      task.value.onUpdate(ValueListener)
    }
  }

  private object ValueListener extends (T => Unit) {
    def apply(value: T) {
      update(value)
    }
  }
}

/**
 * Listens to the current status of the current users task.
 */
class CurrentTaskStatusListener[TaskType <: HasStatus](userData: TaskData[TaskType]) extends Listener[TaskStatus] with HasStatus {
  updateStatus(userData().status)
  userData.onUpdate(Listener)
  statusLogLevel = Level.FINEST
  progressLogLevel = Level.FINEST

  @volatile protected var task = userData()

  private object Listener extends (TaskType => Unit) {
    def apply(newTask: TaskType) {
      task = newTask
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