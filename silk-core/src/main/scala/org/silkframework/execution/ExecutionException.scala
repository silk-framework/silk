package org.silkframework.execution

/**
  * Thrown if a problem occurred during the execution of a task.
  */
case class ExecutionException(message: String, cause: Option[Throwable] = None) extends RuntimeException(message, cause.orNull)
