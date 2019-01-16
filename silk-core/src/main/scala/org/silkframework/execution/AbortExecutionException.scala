package org.silkframework.execution

/**
  * Thrown if a fatal problem occurred during the execution.
  * The execution must be aborted after throwing this exception.
  *
  * @param message User-friendly error message
  * @param cause Cause of the exception, if any
  */
case class AbortExecutionException(message: String, cause: Option[Throwable] = None)
  extends ExecutionException(message, cause, abortExecution = true)
