package org.silkframework.entity.metadata

import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import ExceptionSerializer._

import scala.xml.{Elem, Node}

/**
  * XML serializer for exceptions
  * NOTE: use [[readException]] and [[write]] to reference more specific Serializers for subclasses of Throwable
  */
case class ExceptionSerializer() extends XmlMetadataSerializer[Throwable] {

  override def read(ex: Node)(implicit readContext: ReadContext): Throwable = readException(ex)

  def readException(node: Node): Throwable ={
    if(node == null || node.text.trim.isEmpty)
      return null

    val className = (node \ CLASS).text.trim
    val exceptionClass = Class.forName(className).asInstanceOf[Class[Throwable]]

    //FIXME introduce an automated registry for this switch?
    exceptionClass match{
    //NOTE: insert special Exception reading switch here
    //case ex: SpecialException => readSpecialException(..)
      case _ => readDefaultThrowable(node, exceptionClass)
    }
  }

  /**
    * Here we try to guess the right constructor.
    * Trying out combinations of String (message) and Throwable (cause), then only String.
    * NOTE: some exceptions use thusly typed constructors differently or dont have any of such constructors.
    * Please introduce your own serializer for such exception classes
    * @param node - the XML node
    * @param exceptionClass - the exception class in question
    * @return
    */
  def readDefaultThrowable(node: Node, exceptionClass: Class[Throwable]): Throwable = {
    val message = (node \ MESSAGE).text.trim
    val cause = readException((node \ CAUSE).headOption.flatMap(_.child.headOption).orNull)
    var arguments = Seq[Object]()
    val constructor = if (cause != null) {
      var zw = exceptionClass.getConstructor(classOf[String], classOf[Throwable])
      arguments = Seq(message, cause)
      if (zw == null) {
        zw = exceptionClass.getConstructor(classOf[Throwable], classOf[String])
        arguments = Seq(cause, message)
      }
      zw
    }
    else {
      arguments = Seq(message)
      try {
        exceptionClass.getConstructor(classOf[String])
      }
      catch {
        case ex: java.lang.NoSuchMethodException => null
        case _: Throwable => throw new RuntimeException("Construction of exception representation failed for unknown reasons")
      }
    }

    val exception = if (constructor != null) {
      exceptionClass.cast(constructor.newInstance(arguments: _*))
    }
    else {
      new Exception("Emulated Exception of class: " + exceptionClass.getCanonicalName + ", original message: " + message, cause)
    }
    exception.setStackTrace(readStackTrace((node \ STACKTRACE).head))
    exception
  }

  def readStackTrace(node: Node): Array[StackTraceElement] ={
    val stackTrace = for(ste <- node \ STELEMENT) yield{
      val className = (ste \ CLASSNAME).text.trim
      val methodName = (ste \ METHODNAME).text.trim
      val fileName = (ste \ FILENAME).text.trim
      val lineNumber = (ste \ LINENUMBER).text.trim
      new StackTraceElement(className, methodName, fileName, if(lineNumber.length > 0) lineNumber.toInt else 0)
    }
    stackTrace.toArray
  }

  override def write(ex: Throwable)(implicit writeContext: WriteContext[Node]): Node = ex.getClass match{
    //FIXME introduce an automated registry for this switch?
  //case se: SpecialException => writeSpecialException(..)
    case _ => writeException(ex)
  }

  private def writeException(ex: Throwable): Node ={
    <Exception>
      <Class>{ex.getClass.getCanonicalName}</Class>
      <Message>{ex.getMessage}</Message>
      <Cause>
        {if(ex.getCause != null) writeException(ex.getCause)}
      </Cause>
      <StackTrace>
        {writeStackTrace(ex)}
      </StackTrace>
    </Exception>
  }

  private def writeStackTrace(ex: Throwable): Array[Elem] ={
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
    * The identifier used to define metadata objects in the map of [[EntityMetadata]]
    */
  override def metadataId: String = ExceptionSerializer.ID

  /**
    * An indicator whether the LazyMetadata object produced with this serializer will be replaceable (overridable in the Metadata map)
    *
    * @return
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
