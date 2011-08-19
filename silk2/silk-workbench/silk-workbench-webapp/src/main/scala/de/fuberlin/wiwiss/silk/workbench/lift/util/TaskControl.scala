package de.fuberlin.wiwiss.silk.workbench.lift.util

import de.fuberlin.wiwiss.silk.util.task.{Status, Finished, Started, Task}

class TaskControl(task: Task[_], cancelable: Boolean = false) extends DynamicButton {

  task.onUpdate(TaskListener)

  label = "Start"

  override protected def onPressed() = {
    if (!task.isRunning) {
      task.runInBackground()
    } else if (cancelable) {
      task.cancel()
    }
    JS.Empty
  }

  private object TaskListener extends (Status => Unit) {
    def apply(status: Status) {
      status match {
        case _: Started if cancelable => label = "Stop"
        case _: Started => label = "Start"
        case _: Finished => label = "Start"
        case _ =>
      }
    }
  }
}