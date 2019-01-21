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
case class GenericExecutionFailure(message: String,
                                   cause: Option[GenericExecutionFailure],
                                   className: Option[String]
                           ) {
// TODO Rename class or make it a Throwable if you like. Throwable would make sense, but Markus and Andreas were against it
  /**
    * Constructor that copiess the values of a Throwable.
    *
    * @param t Throwable
    * @return GenericExecutionFailure
    */
  def apply(t: Throwable): GenericExecutionFailure = {
    GenericExecutionFailure.fromThrowable(t)
  }

  var stackTrace: Option[Array[StackTraceElement]] = None

  /**
    * Set the stacktrace after the class instantiation.
    *
    * @param st Array of StackTraceElement objects
    */
  def setStackTrace(st: Array[StackTraceElement]): Unit = {
    stackTrace = Some(st)
  }

  /**
    * Get the stack trace.
    *
    * @return Array of StackTraceElement or an empty array.
    */
  def getStackTrace: Array[StackTraceElement] = stackTrace.getOrElse(Array.empty)

  def getMessage: String = {
    if (message == null || message.trim.isEmpty) {
      GenericExecutionFailure.NOMSG + " " + GenericExecutionFailure.messageOriginClass(className)
    }
    else {
      message
    }
  }

  /**
    * Get the class name of the original Exception. The String will be an error
    * message, if the class does not exist.
    *
    * @return class name od [[GenericExecutionFailure.NOMSG]]
    */
  def getExceptionClass: String = className.getOrElse(GenericExecutionFailure.NOCLA)

  def getCause: Option[GenericExecutionFailure] = cause
}

/**
  * Some helper methods for JSON and XML serialization.
  */
object GenericExecutionFailure {

  /* messages */
  final val NOMSG: String = "The exception message was empty or incorrectly de/serialized."
  final val NOCLA: String = "The exception class does not exist or was incorrectly de/serialized."
  def messageOriginClass(className: Option[String]): String = {
    s"The origin class was: ${className.getOrElse("unknown")}"
  }

  /**
    * Basic ExecutionFailure instance with only a message.
    *
    * @param message Error message
    * @return
    */
  def noInformationFailure(message: String): GenericExecutionFailure = {
    GenericExecutionFailure(
      message = message,
      cause = None,
      className = None
    )
  }

  /**
    * Get the Throwable that is represented byy the [[GenericExecutionFailure]].
    *
    * @param ef GenericFailure
    * @return
    */
  def asThrowable(ef: GenericExecutionFailure): Throwable = {
    GenericExecutionException(ef.getMessage, ef.getCause.map(asThrowable), Some(ef.getCause.getClass.getName), Some(ef.getStackTrace))
  }

  /**
    * Create a [[GenericExecutionFailure]] object from a Throwable.
    *
    * @param t Throwable
    * @return
    */
  def fromThrowable(t: Throwable): GenericExecutionFailure = {
    val executionFailure = if (t.getCause == null) {
      GenericExecutionFailure(
        t.getMessage,
        None,
        Some(t.getClass.getName)
      )
    }
    else if (t.getCause == t) {
      GenericExecutionFailure(
        t.getMessage,
        None,
        Some(t.getClass.getName)
      )
    }
    else {
      GenericExecutionFailure(
        t.getMessage,
        Some(fromThrowable(t.getCause)),
        Some(t.getClass.getName)
      )
    }
    if (t.getStackTrace != null) executionFailure.setStackTrace(t.getStackTrace)
    executionFailure
  }

  /**
    * Generic Throwable with a message and optional a cause. Used when no underlying throwable of a
    * [[GenericExecutionFailure]] is found.
    * Optionally can contain the original exception class name.
    *
    * Only exists for compatibility with [[org.silkframework.failures.FailureClass]]
    *
    * @param message Error message
    * @param cause Error cause
    */
  case class GenericExecutionException(message: String, cause: Option[Throwable], exceptionClass: Option[String] = None,
                                       stackTrace: Option[Array[StackTraceElement]] = None) extends Throwable {

    /**
      * Required for call in [[org.silkframework.failures.FailureClass]]
      *
      * @return
      */
    override def getMessage: String = message

    override def getStackTrace: Array[StackTraceElement] = stackTrace.getOrElse(Array[StackTraceElement]())
  }

}