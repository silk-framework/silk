package org.silkframework.serialization.json.metadata

import java.lang.reflect.Constructor
import org.silkframework.entity.metadata.{EntityMetadata, ExceptionSerializer}
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.serialization.json.JsonHelpers._
import play.api.libs.json._
import scala.util.Try

case class ExceptionSerializerJson() extends JsonMetadataSerializer[Throwable] {

  override def read(ex: JsValue)(implicit readContext: ReadContext): Throwable = readException(ex)

  def readException(node: JsValue): Throwable ={
    node match {
      case JsNull => null
      case JsObject(_) =>
        val className = stringValue(node, ExceptionSerializer.CLASS)
        val message = Try{stringValue(node, ExceptionSerializer.MESSAGE)}.toOption
        val cause = (node \ ExceptionSerializer.CAUSE).toOption match {
          case Some(c) => readException(c)
          case None => null
        }

        val exceptionClass = Class.forName(className).asInstanceOf[Class[Throwable]]
        var arguments = Seq[Object]()
        val constructor: Constructor[Throwable] = if (cause != null) {
          var zw = exceptionClass.getConstructor(classOf[String], classOf[Throwable])
          arguments = Seq(message.orNull, cause)
          if (zw == null) {
            zw = exceptionClass.getConstructor(classOf[Throwable], classOf[String])
            arguments = Seq(cause, message.orNull)
          }
          zw
        }
        else {
          arguments = Seq(message.orNull)
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
          new Exception("Emulated Exception of class: " + className + ", original message: " + message.orNull, cause)
        }
        exception.setStackTrace(mustBeJsArray((node \ ExceptionSerializer.STACKTRACE).getOrElse(JsArray(Seq())))(readStackTrace))
        exception

      case _ => throw new IllegalArgumentException("Neither JsNull nor JsObject was found, representing an Exception.")
    }
  }

  def readStackTrace(node: JsArray): Array[StackTraceElement] ={
    val stackTrace = for(ste <- node.value) yield{

      val className = stringValue(ste, ExceptionSerializer.CLASSNAME)
      val methodName = stringValue(ste, ExceptionSerializer.METHODNAME)
      val fileName = stringValue(ste, ExceptionSerializer.FILENAME)
      val lineNumber = numberValue(ste, ExceptionSerializer.LINENUMBER)
      new StackTraceElement(className, methodName, fileName, if(lineNumber != null) lineNumber.toInt else 0)
    }
    stackTrace.toArray
  }

  override def write(ex: Throwable)(implicit writeContext: WriteContext[JsValue]): JsValue = writeException(ex)

  private def writeException(ex: Throwable): JsObject ={
    JsObject(
      Seq(
        ExceptionSerializer.CLASS -> JsString(ex.getClass.getCanonicalName),
        ExceptionSerializer.MESSAGE -> JsString(ex.getMessage),
        ExceptionSerializer.CAUSE -> (if(ex.getCause != null) writeException(ex.getCause) else JsNull),
        ExceptionSerializer.STACKTRACE -> writeStackTrace(ex)
      )
    )
  }

  private def writeStackTrace(ex: Throwable): JsArray ={
    val arr = for(ste <- ex.getStackTrace) yield{
      JsObject(Seq(
        ExceptionSerializer.FILENAME -> JsString(ste.getFileName),
        ExceptionSerializer.CLASSNAME -> JsString(ste.getClassName),
        ExceptionSerializer.METHODNAME -> JsString(ste.getMethodName),
        ExceptionSerializer.LINENUMBER -> JsNumber(ste.getLineNumber)
      ))
    }
    JsArray(arr)
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
