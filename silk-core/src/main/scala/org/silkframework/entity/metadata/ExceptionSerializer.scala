package org.silkframework.entity.metadata

import org.silkframework.runtime.serialization.{ReadContext, WriteContext}

import scala.xml.{Elem, Node}

case class ExceptionSerializer() extends XmlMetadataSerializer[Throwable] {

  override def read(ex: Node)(implicit readContext: ReadContext): Throwable = readException(ex)

  def readException(node: Node): Throwable ={
    if(node == null)
      return null

    val className = (node \ "Class").text.trim
    val message = (node \ "Message").text.trim
    val cause = readException((node \ "Cause").headOption.flatMap(_.child.headOption).orNull)

    val exceptionClass = Class.forName(className).asInstanceOf[Class[Throwable]]
    var arguments = Seq[Object]()
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
    exception.setStackTrace(readStackTrace((node \ "StackTrace").head))
    exception
  }

  def readStackTrace(node: Node): Array[StackTraceElement] ={
    val stackTrace = for(ste <- node \ "STE") yield{
      val className = (ste \ "ClassName").text.trim
      val methodName = (ste \ "MethodName").text.trim
      val fileName = (ste \ "FileName").text.trim
      val lineNumber = (ste \ "LineNumber").text.trim
      new StackTraceElement(className, methodName, fileName, if(lineNumber.length > 0) lineNumber.toInt else 0)
    }
    stackTrace.toArray
  }

  override def write(ex: Throwable)(implicit writeContext: WriteContext[Node]): Node = writeException(ex)

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
  override val metadataId: String = ExceptionSerializer.ID
}

object ExceptionSerializer{
  val ID: String = EntityMetadata.FAILURE_KEY
}