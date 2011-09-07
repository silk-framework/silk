package de.fuberlin.wiwiss.silk.workbench.lift.util

import de.fuberlin.wiwiss.silk.util.task.{TaskStatus, TaskFinished, TaskStarted, Task}

class TaskControl(task: Task[_], cancelable: Boolean = false) extends DynamicButton {

  task.onUpdate(TaskListener)

  label = "Start"

  override protected def onPressed() = {
    if (!task.status.isRunning) {
      task.runInBackground()
    } else if (cancelable) {
      task.cancel()
    }
    JS.Empty
  }

  private object TaskListener extends (TaskStatus => Unit) {
    def apply(status: TaskStatus) {
      status match {
        case _: TaskStarted if cancelable => label = "Stop"
        case _: TaskStarted => label = "Start"
        case _: TaskFinished => label = "Start"
        case _ =>
      }
    }
  }
}