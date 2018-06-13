package org.silkframework.workspace

import org.silkframework.runtime.validation.NotFoundException

/**
  * Thrown if a task could not be found.
  *
  * @param projectName The project.
  * @param taskName The name of the task.
  * @param taskType The type of the task, e.g., Dataset
  */
case class TaskNotFoundException(projectName: String, taskName: String, taskType: String) extends

  NotFoundException(s"${taskType.capitalize} '$taskName' not found in project '$projectName'") {

  override val errorTitle = "Task not found"

}
