package de.fuberlin.wiwiss.silk.workbench.lift.util

import collection.mutable.{Publisher, Subscriber}
import de.fuberlin.wiwiss.silk.util.task.{Status, Finished, Started, Task}

class TaskControl(task: Task[_], cancelable: Boolean = false) extends DynamicButton with Subscriber[Status, Publisher[Status]] {

  task.subscribe(this)

  label = "Start"

  override def notify(pub: Publisher[Status], status: Status) {
    status match {
      case _: Started if cancelable => label = "Stop"
      case _: Started => label = "Start"
      case _: Finished => label = "Start"
      case _ =>
    }
  }

  override protected def onPressed() {
    if (!task.isRunning) {
      task.runInBackground()
    } else if (cancelable) {
      task.cancel()
    }
  }
}