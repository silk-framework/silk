package models

import java.util.logging.Level
import de.fuberlin.wiwiss.silk.runtime.task._

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