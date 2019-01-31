package org.silkframework.execution

/**
  * Thrown if a problem occurred during the execution of a task.
  *
  * @param message User-friendly error message
  * @param cause Cause of the exception, if any
  * @param abortExecution If true, this is a fatal exception that should abort the execution.
  */
class ExecutionException(message: String, cause: Option[Throwable], val abortExecution: Boolean) extends RuntimeException(message, cause.orNull)
