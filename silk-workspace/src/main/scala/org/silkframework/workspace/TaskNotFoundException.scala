package org.silkframework.workspace

case class TaskNotFoundException(projectName: String, taskName: String, taskType: String) extends NoSuchElementException(s"Task '$taskName' of type $taskType not found in $projectName")
