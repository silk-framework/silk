package org.silkframework.entity.metadata

import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.slf4j.LoggerFactory

import scala.util.Try
import scala.xml.{Elem, Node}

/**
  * XML serializer for exceptions.
  * Update: Map subclasses of throwable to the ExecutionFailure class and serialize/instantiate that.
  * Custom serializers was planned are not needed any longer.
  */
case class ExceptionSerializer() extends XmlMetadataSerializer[GenericExecutionFailure] {

  @transient
  private val logger = LoggerFactory.getLogger(this.getClass)

  override def read(ex: Node)(implicit readContext: ReadContext): GenericExecutionFailure = readException(ex)

  /**
    * Deserialize and instantiate an error. This used to recommend an automated registry and custom Serializers.
    * It then searched certain constructors and tried to instantiate the Throwable.
    *
    * Now we only instantiate an ExecutionFailure. This also avoids the emulated exceptions that sometimes
    * were necessary.
    *
    * @param node XML node
    * @return ExecutionFailure instance
    */
  def readException(node: Node): GenericExecutionFailure = {

    if (node == null || node.text.trim.isEmpty) {
      return null
    }

    // get name, if empty we just return the most basic ExecutionFailure
    val className = Try((node \ ExceptionSerializer.CLASS).text.trim).toOption
    val message = (node \ ExceptionSerializer.MESSAGE).text.trim
    if (className.isEmpty) {
      logger.warn("The deserialized exception does not have a class")
      logger.warn(s"Message was: $message")
      GenericExecutionFailure.noInformationFailure(message)
    }
    else {
      readDefaultThrowable(node)
    }
  }

  /**
    * Finds a fitting constructor and returns the instantiated exception object.
    *
    * @param node - the XML node
    * @return
    */
  def readDefaultThrowable(node: Node): GenericExecutionFailure = {
    val failure: GenericExecutionFailure = try {
      val exceptionClass = (node \ ExceptionSerializer.CLASS).text.trim
      val message = (node \ ExceptionSerializer.MESSAGE).text.trim
      val cause = getExceptionCauseOption(node)
      GenericExecutionFailure(message, cause, Some(exceptionClass))
    }
    catch {
      case t: Throwable =>
        GenericExecutionFailure.noInformationFailure("There was a failure while gathering the information about the error." +
          s" Reason: ${t.getMessage}")
    }

    if ((node \ ExceptionSerializer.STACKTRACE).nonEmpty) {
      failure.setStackTrace(readStackTrace((node \ ExceptionSerializer.STACKTRACE).head))
    }
    failure
  }

  /**
    * Method to help retrieve optional causes.
    *
    * @param node XML node
    * @return
    */
  private def getExceptionCauseOption(node: Node): Option[GenericExecutionFailure] = {
    val cause = readException((node \ ExceptionSerializer.CAUSE).headOption.flatMap(_.child.headOption).orNull)
    Option(cause)
  }

  def readStackTrace(node: Node): Array[StackTraceElement] = {
    val stackTrace = for (ste <- node \ ExceptionSerializer.STELEMENT) yield {
      val className = (ste \ ExceptionSerializer.CLASSNAME).text.trim
      val methodName = (ste \ ExceptionSerializer.METHODNAME).text.trim
      val fileName = (ste \ ExceptionSerializer.FILENAME).text.trim
      val lineNumber = (ste \ ExceptionSerializer.LINENUMBER).text.trim
      new StackTraceElement(className, methodName, fileName, if (lineNumber.length > 0) lineNumber.toInt else -1)
    }
    stackTrace.toArray
  }

  override def write(value: GenericExecutionFailure)(implicit writeContext: WriteContext[Node]): Node = writeException(value)

  private def writeException(ex: GenericExecutionFailure): Node = {
    <Exception>
      <Class>{ex.getExceptionClass}</Class>
      <Message>{ex.getMessage}</Message>
      <Cause>
      {
        if(ex.getCause.isEmpty) {
          ""
        }
        else {
          writeException(ex.getCause.get)
        }
      }
      </Cause>
      <StackTrace>
        {writeStackTrace(ex)}
      </StackTrace>
    </Exception>
  }

  private def writeStackTrace(ex: GenericExecutionFailure): Array[Elem] ={
    for(ste <- ex.getStackTrace) yield{
      <STE>
        <FileName>{ste.getFileName}</FileName>
        <ClassName>{ste.getClassName}</ClassName>
        <MethodName>{ste.getMethodName}</MethodName>
        <LineNumber>{ste.getLineNumber}</LineNumber>
      </STE>
    }
  }

  /**
    * The identifier used to define metadata objects in the map of [[org.silkframework.entity.metadata.EntityMetadata]]
    * NOTE: This method has to be implemented as def and not as val, else the serialization format registration will fail !!!!!!!!!
    */
  override def metadataId: String = ExceptionSerializer.ID

  /**
    * An indicator whether the LazyMetadata object produced with this serializer will be replaceable (overridable in the Metadata map)
    */
  override def replaceableMetadata: Boolean = false

}

object ExceptionSerializer{
  val ID: String = "exception_class"
  val EXCEPTION: String = "Exception"
  val CLASS: String = "Class"
  val MESSAGE: String = "Message"
  val CAUSE: String = "Cause"
  val STACKTRACE: String = "StackTrace"
  val STELEMENT: String = "STE"
  val FILENAME: String = "FileName"
  val CLASSNAME: String = "ClassName"
  val METHODNAME: String = "MethodName"
  val LINENUMBER: String = "LineNumber"
}
