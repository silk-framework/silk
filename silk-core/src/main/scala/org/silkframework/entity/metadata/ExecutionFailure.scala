package org.silkframework.entity.metadata

/**
  * This case class describes exceptions that can occur during a workflow execution or general task execution
  * and are stored for each processed entity.
  * The serialization/deserialization of exceptions that are unknown beforehand has some disadvantages:
  * - Errors when exceptions with non-standard constructors occur
  * - Problems with reflection in general
  * - Problems with exceptions that don't exist on the application classpath (can occur in some cluster deployments)
  *
  * The exceptions are therefore wrapped in this easily serializable case class. This reduces the serialization problem
  * to a mapping of exceptions to this class. This class is not a subclass of throwable and should not be thrown anywhere.
  *
  * However, since the serializers are called from various places and expect a Throwable an asThrowable method is provided.
  * Like wise this object can be instantiated with a given Throwable.
  *
  * A message does not necessarily exist. In the ExecutionFailure it is mandatory. It should always at least have a message
  * saying that no information could be found.
  *
  * Class, method can be empty. This happened with some exceptions thrown by Spark. A line number might also be missing
  * in exceptions. If that is the case the default of "-1".
  * If the cause of an exception is null, it means there is not other underlying failure. This is valid and represented
  * by None in the ExecutionFailure.
  *
  * @param message    optional message
  * @param className  optional exception class name
  * @param cause      optional failure that is the reason for this one
  */
case class ExecutionFailure( message: String,
                             cause: Option[ExecutionFailure],
                             className: Option[String]
                           ) {

  def apply(t: Throwable): ExecutionFailure = {
    ExecutionFailure.fromThrowable(t)
  }

  var stackTrace: Option[Array[StackTraceElement]] = None
  def setStackTrace(st: Array[StackTraceElement]): Unit = {
    stackTrace = Some(st)
  }

  def getStackTrace: Array[StackTraceElement] = stackTrace.getOrElse(Array.empty)

  def getMessage: String = {
    if (message == null || message.trim.isEmpty) {
      ExecutionFailure.NOMSG + " " + ExecutionFailure.messageOriginClass(className)
    }
    else {
      message
    }
  }

  def getExceptionClass: String = className.getOrElse(ExecutionFailure.NOCLA)

  def getCause: Option[ExecutionFailure] = cause
}

/**
  * Some helper methods for JSON and XML serialization.
  */
object ExecutionFailure {


  /* messages */
  final val NOMSG: String = "The exception message was empty or incorrectly de/serialized."
  final val NOCLA: String = "The exception class does not exist or was incorrectly de/serialized."
  def messageOriginClass(className: Option[String]): String = {
    s"The origin class was: ${className.getOrElse("unknown")}"
  }

  /**
    * Basic ExecutionFailure instance with only an optional message
    *
    * @param message Error message
    * @return
    */
  def noInformationFailure(message: String): ExecutionFailure = {
    ExecutionFailure(
      message = message,
      cause = None,
      className = None
    )
  }

  def asThrowable(ef: ExecutionFailure): Throwable = {
    ExecutionException(ef.getMessage, ef.getCause.map(asThrowable))
  }

  def fromThrowable(t: Throwable): ExecutionFailure = {
    val executionFailure = if (t.getCause == null) {
      ExecutionFailure(t.getMessage, None, Some(t.getClass.getSimpleName))
    }
    else {
      ExecutionFailure(t.getMessage, Some(fromThrowable(t.getCause)), Some(t.getClass.getSimpleName))
    }
    if (t.getStackTrace != null) executionFailure.setStackTrace(t.getStackTrace)
    executionFailure
  }

  case class ExecutionException(message: String, cause: Option[Throwable]) extends Throwable

}