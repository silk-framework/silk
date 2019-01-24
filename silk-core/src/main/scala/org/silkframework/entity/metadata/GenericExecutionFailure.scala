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
  * @param className  exception class name
  * @param cause      optional failure that is the reason for this one
  * @param stackTrace optional stack trace
  */
case class GenericExecutionFailure(message: Option[String],
                                   className: String,
                                   cause: Option[GenericExecutionFailure],
                                   stackTrace: Option[Array[StackTraceElement]]) {
  def getStackTrace: Array[StackTraceElement] = stackTrace.getOrElse(Array.empty)

  def getMessage: String = message.orNull

  /**
    * Get the class name of the original Exception.
    *
    * @return class name
    */
  def getExceptionClass: String = className
}

/**
  * Some helper methods for JSON and XML serialization.
  */
object GenericExecutionFailure {

  def apply(message: String, className: String): GenericExecutionFailure = {
    GenericExecutionFailure(Some(message), className, None, None)
  }

  /**
    * Get the Throwable that is represented byy the [[GenericExecutionFailure]].
    *
    * @param ef GenericFailure
    * @return
    */
  def asThrowable(ef: GenericExecutionFailure): Throwable = {
    GenericExecutionException(ef.getMessage, ef.cause.map(asThrowable), Some(ef.cause.getClass.getName), Some(ef.getStackTrace))
  }

  /**
    * Create a [[GenericExecutionFailure]] object from a Throwable.
    *
    * @param t Throwable
    * @return
    */
  def apply(t: Throwable): GenericExecutionFailure = {
    val cause = if (t.getCause != null) {
      Some(apply(t.getCause))
    } else {
      None
    }
    val message = Option(t.getMessage)
    val className = t.getClass.getName
    val stackTrace = Option(t.getStackTrace)
    GenericExecutionFailure(message, className, cause, stackTrace)
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