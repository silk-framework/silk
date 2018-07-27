package org.silkframework.entity.metadata

import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import ExceptionSerializer._

import scala.xml.{Elem, Node}

case class ExceptionSerializer() extends XmlMetadataSerializer[Throwable] {

  override def read(ex: Node)(implicit readContext: ReadContext): Throwable = readException(ex)

  def readException(node: Node): Throwable ={
    if(node == null)
      return null

    val className = (node \ CLASS).text.trim
    val message = (node \ MESSAGE).text.trim
    val cause = readException((node \ CAUSE).headOption.flatMap(_.child.headOption).orNull)

    val exceptionClass = Class.forName(className).asInstanceOf[Class[Throwable]]
    var arguments = Seq[Object]()
    //TODO this is not deterministic, there might be other constructors
    val constructor = if(cause != null){
      var zw = exceptionClass.getConstructor(classOf[String], classOf[Throwable])
      arguments = Seq(message, cause)
      if(zw == null) {
        zw = exceptionClass.getConstructor(classOf[Throwable], classOf[String])
        arguments = Seq(cause, message)
      }
      zw
    }
    else{
      arguments = Seq(message)
      exceptionClass.getConstructor(classOf[String])
    }

    val exception = if(constructor != null){
      exceptionClass.cast(constructor.newInstance(arguments:_*))
    }
    else{
      new Exception("Emulated Exception of class: " + className + ", original message: " + message, cause)
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

  override def write(ex: Throwable)(implicit writeContext: WriteContext[Node]): Node = writeException(ex)

  //TODO parameterize tag names
  private def writeException(ex: Throwable): Node ={
    <Exception>
      <Class>{ex.getClass.getCanonicalName}</Class>
      <Message>{ex.getMessage}</Message>
      <Cause>{if(ex.getCause != null) writeException(ex.getCause)}</Cause>
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