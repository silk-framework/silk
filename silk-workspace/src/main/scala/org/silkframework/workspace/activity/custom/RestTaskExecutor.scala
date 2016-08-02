package org.silkframework.workspace.activity.custom

import org.silkframework.config.CustomTask
import org.silkframework.plugins.custom.net.RestTaskSpec
import org.silkframework.runtime.activity.{Activity, ActivityContext}
import org.silkframework.workspace.ProjectTask

/**
  * Created on 8/2/16.
  */
case class RestTaskExecutor(task: ProjectTask[CustomTask]) extends Activity[Unit] {
  /**
    * Executes this activity.
    *
    * @param context Holds the context in which the activity is executed.
    */
  override def run(context: ActivityContext[Unit]): Unit = {
    val restTaskSpec = {
      task.data match {
        case s: RestTaskSpec => s
        case _ => throw new UnsupportedOperationException("This is not a REST task")
      }
    }
  }
}
