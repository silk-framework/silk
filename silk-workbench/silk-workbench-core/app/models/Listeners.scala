package models

import de.fuberlin.wiwiss.silk.runtime.activity.{ValueHolder, ActivityControl, Status}

abstract class TaskDataListener[T](userData: TaskData[T]) extends Listener[T] {
  userData.onUpdate(Listener)

  private object Listener extends (T => Unit) {
    def apply(value: T) {
      apply(value)
    }
  }
}

/**
 * Listens to the current status of the current users task.
 */
abstract class CurrentStatusListener(taskHolder: TaskData[ActivityControl[_]]) extends Listener[Status] {

  //Deactivate logging
  //TODO statusLogLevel = Level.FINEST
  // progressLogLevel = Level.FINEST

  val statusHolder = new ValueHolder[Status](Some(Status.Idle))

  //Listen to changes of the current task
  taskHolder.onUpdate(Listener)

  //Set current task
  @volatile protected var task = taskHolder()

  //Listen to changes of the status of the current task.
  task.status.onUpdate(StatusListener)
  statusHolder.update(task.status())

  private object Listener extends (ActivityControl[_] => Unit) {
    def apply(newActivity: ActivityControl[_]) {
      task = newActivity
      task.status.onUpdate(StatusListener)
    }
  }

  private object StatusListener extends (Status => Unit) {
    def apply(status: Status) {
      apply(status)
      statusHolder.update(status)
    }
  }
}

/**
 * Listens to the current status of the current users task.
 */
abstract class CurrentActivityStatusListener[TaskType <: ActivityControl[_]](taskHolder: TaskData[TaskType]) extends Listener[Status] {

  //Listen to changes of the current task
  taskHolder.onUpdate(Listener)

  //Set current task
  @volatile protected var taskData = taskHolder()

  //Listen to changes of the status of the current task.
  taskData.status.onUpdate(StatusListener)

  private object Listener extends (TaskType => Unit) {
    def apply(newTask: TaskType) {
      taskData = newTask
      taskData.status.onUpdate(StatusListener)
    }
  }

  private object StatusListener extends (Status => Unit) {
    def apply(status: Status) {
      apply(status)
    }
  }
}