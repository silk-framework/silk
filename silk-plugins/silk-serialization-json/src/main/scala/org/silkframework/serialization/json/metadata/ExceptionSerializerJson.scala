package org.silkframework.serialization.json.metadata

import org.silkframework.entity.metadata.{ExceptionSerializer, GenericExecutionFailure}
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.serialization.json.JsonHelpers._
import org.slf4j.LoggerFactory
import play.api.libs.json._

import scala.util.Try

case class ExceptionSerializerJson() extends JsonMetadataSerializer[GenericExecutionFailure] {

  @transient
  private val logger = LoggerFactory.getLogger(this.getClass)

  override def read(ex: JsValue)(implicit readContext: ReadContext): GenericExecutionFailure = readException(ex)

  def readException(jsValue: JsValue): GenericExecutionFailure = {
    jsValue match {
      case JsNull => null
      case JsObject(_) =>
        val message = stringValueOption(jsValue, ExceptionSerializer.MESSAGE)
        val className = stringValue(jsValue, ExceptionSerializer.CLASSNAME)
        val cause: Option[GenericExecutionFailure] = getExceptionCauseOption(jsValue)
        val stackTrace = arrayValueOption(jsValue, ExceptionSerializer.STACKTRACE).map(readStackTrace)
        GenericExecutionFailure(message, className, cause, stackTrace)
      case _ => throw new IllegalArgumentException("Neither JsNull nor JsObject was found, representing an Exception.")
    }
  }

  private def getExceptionCauseOption(node: JsValue): Option[GenericExecutionFailure] = {
    optionalValue(node, ExceptionSerializer.CAUSE) map { c =>
      readException(c)
    }
  }

  def readStackTrace(node: JsArray): Array[StackTraceElement] = {
    val stackTrace = for (ste <- node.value) yield {
      val className = stringValueOption(ste, ExceptionSerializer.CLASSNAME).getOrElse("unknown")
      val methodName = stringValueOption(ste, ExceptionSerializer.METHODNAME).getOrElse("unknown")
      val fileName = stringValueOption(ste, ExceptionSerializer.FILENAME).getOrElse("unknown")
      val lineNumber = numberValueOption(ste, ExceptionSerializer.LINENUMBER).map(_.intValue()).getOrElse(-1)
      new StackTraceElement(className, methodName, fileName, lineNumber)
    }
    stackTrace.toArray
  }

  override def write(ef: GenericExecutionFailure)(implicit writeContext: WriteContext[JsValue]): JsValue = writeException(ef)

  private def writeException(ef: GenericExecutionFailure): JsObject = {
    JsObject(
      Seq(
        ExceptionSerializer.CLASSNAME -> JsString(ef.className)
      ) ++ Seq(
        ef.message.map(msg => ExceptionSerializer.MESSAGE -> JsString(msg)),
        ef.cause.map(cause => ExceptionSerializer.CAUSE -> writeException(cause)),
        ef.stackTrace.map(stackTrace => ExceptionSerializer.STACKTRACE -> writeStackTrace(stackTrace))
      ).flatten
    )
  }

  private def writeStackTrace(stacktrace: Array[StackTraceElement]): JsArray = {
    val arr = for (ste <- stacktrace) yield {
      JsObject(Seq(
        ExceptionSerializer.FILENAME -> JsString(ste.getFileName),
        ExceptionSerializer.CLASSNAME -> JsString(ste.getClassName),
        ExceptionSerializer.METHODNAME -> JsString(ste.getMethodName),
        ExceptionSerializer.LINENUMBER -> JsNumber(ste.getLineNumber)
      ))
    }
    JsArray(arr)
  }

  override def metadataId: String = ExceptionSerializer.ID

  override def replaceableMetadata: Boolean = false
}

