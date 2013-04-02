package models

import de.fuberlin.wiwiss.silk.util.Observable
import java.util.logging.Logger
import java.util.concurrent.Executors
import de.fuberlin.wiwiss.silk.util.task.ValueTask
import java.util.logging.Level
import java.util.concurrent.TimeUnit
import de.fuberlin.wiwiss.silk.util.task.HasStatus
import de.fuberlin.wiwiss.silk.util.task.TaskStatus

//TODO write tests and move to its own file
trait Listener[T]{

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

  protected def onUpdate(value: T)

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

abstract class TaskStatusListener(task: HasStatus) extends Listener[TaskStatus] {
  task.onUpdate(Listener)

  private object Listener extends (TaskStatus => Unit) {
    def apply(status: TaskStatus) {
      update(status)
    }
  }
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
abstract class CurrentTaskValueListener[T](taskHolder: TaskData[_ <: ValueTask[T]]) extends Listener[T] {
  taskHolder.onUpdate(Listener)
  taskHolder().value.onUpdate(ValueListener)

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
abstract class CurrentTaskStatusListener[TaskType <: HasStatus](taskHolder: TaskData[TaskType]) extends Listener[TaskStatus] with HasStatus {

  //Deactivate logging
  statusLogLevel = Level.FINEST
  progressLogLevel = Level.FINEST

  //Listen to changes of the current task
  taskHolder.onUpdate(Listener)

  //Set current task
  @volatile protected var task = taskHolder()

  //Listen to changes of the status of the current task.
  task.onUpdate(StatusListener)
  updateStatus(task.status)

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