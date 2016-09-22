package org.silkframework.execution

/**
  * Runtime exception that signals that something unexpected went wrong in a task, which might be temporary.
  * A workflow could retry a task that has thrown this exception.
  */
case class TaskException(errorMsg: String) extends RuntimeException(errorMsg)
