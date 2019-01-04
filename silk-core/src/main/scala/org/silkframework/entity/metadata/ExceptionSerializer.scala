package org.silkframework.entity.metadata

import java.lang.reflect.Constructor

import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import ExceptionSerializer._
import org.slf4j.LoggerFactory

import scala.Option
import scala.xml.{Elem, Node}

/**
  * XML serializer for exceptions
  * NOTE: use [[readException]] and [[write]] to reference more specific Serializers for subclasses of Throwable
  */
case class ExceptionSerializer() extends XmlMetadataSerializer[Throwable] {

  @transient
  private val logger = LoggerFactory.getLogger(this.getClass)

  override def read(ex: Node)(implicit readContext: ReadContext): Throwable = readException(ex)

  def readException(node: Node): Throwable ={

    node match {
      case null =>
        null
      case _ =>
        val className = (node \ CLASS).text.trim
        val exceptionClass = Class.forName(className).asInstanceOf[Class[Throwable]]

        //FIXME introduce an automated registry for this switch?
        exceptionClass match{
          //NOTE: insert special Exception reading switch here
          //case ex: SpecialException => readSpecialException(..)
          case _ => readDefaultThrowable(node, exceptionClass)
        }
//      case _ =>
//        throw new IllegalArgumentException("Neither JsNull nor JsObject was found, representing an Exception.")
    }

//    if(node == null || node.text.trim.isEmpty) {
//      return null
//    }


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
    val causeOpt = getExceptionCauseOption(node)
    val constructorOpt = getExceptionConstructor(causeOpt, exceptionClass, message, exceptionClass.getSimpleName)

    val exception = if (constructorOpt.nonEmpty) {
      exceptionClass.cast(constructorOpt.get.newInstance(Seq[Object](): _*))
    }
    else {
      new Exception(
        "Emulated Exception of class: " + exceptionClass.getCanonicalName + ", original message: " + message,
        causeOpt.orNull
      )
    }
    exception.setStackTrace(readStackTrace((node \ STACKTRACE).head))
    exception
  }


  /**
    * Method to help retrieve optional causes.
    *
    * @param node
    * @return
    */
  private def getExceptionCauseOption(node: Node): Option[Throwable] = {
    val cause = readException((node \ CAUSE).headOption.flatMap(_.child.headOption).orNull)
    Option(cause)
  }

  /**
    * Some method to determine the constructor of a Throwable with out that being necessarily there.
    * Depending the input, that is largely optional different constructors a searched via reflection.
    *
    * @param cause            Optional Throwable
    * @param exceptionClass   Class
    * @param message          Message
    * @param className        className
    * @return Constructor of a Throwable or None
    */
  def getExceptionConstructor(cause: Option[Throwable], exceptionClass: Class[Throwable], message: String,
                              className: String): Option[Constructor[Throwable]] = {
    try {
      var arguments = Seq[Object]()
      if (cause.nonEmpty) {
        var constructor = exceptionClass.getConstructor(classOf[String], classOf[Throwable])
        arguments = Seq(message, cause)
        if (constructor == null) {
          constructor = exceptionClass.getConstructor(classOf[Throwable], classOf[String])
          arguments = Seq(cause, message)
        }
        Some(constructor)
      }
      else {
        arguments = Seq(message)
        Some(exceptionClass.getConstructor(classOf[String]))
      }
    }
    catch {
      case _: java.lang.NoSuchMethodException => None
      case _: Throwable => throw new RuntimeException(s"Construction of exception $className failed for unknown reasons")
    }
  }



  def readStackTrace(node: Node): Array[StackTraceElement] ={
    val stackTrace = for(ste <- node \ STELEMENT) yield{
      val className = (ste \ CLASSNAME).text.trim
      val methodName = (ste \ METHODNAME).text.trim
      val fileName = (ste \ FILENAME).text.trim
      val lineNumber = (ste \ LINENUMBER).text.trim
      // please review: I did not change from 0 to -1, That seems better, though.
      new StackTraceElement(className, methodName, fileName, if(lineNumber.length > 0) lineNumber.toInt else -1)
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
