package org.silkframework.workspace

import org.silkframework.runtime.validation.NotFoundException

case class TaskNotFoundException(projectName: String, taskName: String, taskType: String) extends
    NotFoundException(s"Task '$taskName' of type $taskType not found in $projectName") {

  override val errorTitle = "Task not found"

}
