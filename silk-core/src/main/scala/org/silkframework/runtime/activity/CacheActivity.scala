package org.silkframework.runtime.activity

import java.util.logging.Logger

import org.silkframework.config.TaskSpec

/**
  * An activity that acts as a cache for task related data.
  */
trait CacheActivity[T <: TaskSpec] { this: Activity[_] =>
  val log: Logger = Logger.getLogger(this.getClass.getName)
  @transient
  var dirty: Boolean = false

  def startDirty(taskActivity: ActivityControl[_]): Unit = {
    dirty = true

    if(taskActivity.status().isRunning) {
      // Do nothing, the dirty flag should be picked up by the activity execution
    } else{
      try {
        taskActivity.start()
      } catch {
        case _: IllegalStateException =>
          // Ignore exception because of race condition that another thread already started the activity
      }
    }
  }
}
