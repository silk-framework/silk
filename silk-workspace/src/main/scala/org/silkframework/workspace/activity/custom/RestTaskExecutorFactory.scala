package org.silkframework.workspace.activity.custom

import org.silkframework.config.CustomTask
import org.silkframework.runtime.activity.Activity
import org.silkframework.runtime.plugin.Plugin
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.activity.TaskActivityFactory

/**
  * Created on 8/2/16.
  */
@Plugin(
  id = "ExecuteRestTask",
  label = "Execute REST Task",
  categories = Array("Custom"),
  description = "Executes the REST task."
)
case class RestTaskExecutorFactory() extends TaskActivityFactory[CustomTask, RestTaskExecutor] {
  override def apply(task: ProjectTask[CustomTask]): Activity[Unit] = {
    new RestTaskExecutor(task)
  }
}
