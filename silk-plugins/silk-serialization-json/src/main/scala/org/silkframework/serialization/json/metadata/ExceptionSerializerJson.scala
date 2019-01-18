package org.silkframework.serialization.json.metadata

import org.silkframework.entity.metadata.{ExceptionSerializer, ExecutionFailure}
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.serialization.json.JsonHelpers._
import org.slf4j.LoggerFactory
import play.api.libs.json._

import scala.util.Try

case class ExceptionSerializerJson() extends JsonMetadataSerializer[ExecutionFailure] {

  @transient
  private val logger = LoggerFactory.getLogger(this.getClass)

  override def read(ex: JsValue)(implicit readContext: ReadContext): ExecutionFailure = readException(ex)

  def readException(node: JsValue): ExecutionFailure = {
    node match {
      case JsNull => null
      case JsObject(_) =>
        val message: String = Try (stringValue(node, ExceptionSerializer.MESSAGE)).getOrElse("no message")
        val className = Try (stringValue(node, ExceptionSerializer.CLASS)).toOption
        val cause: Option[ExecutionFailure] = getExceptionCauseOption(node)
        val exception = if (className.nonEmpty) {
          ExecutionFailure(message, cause, className)
        }
        else {
          logger.warn("The deserialized exception does not have a class")
          logger.warn(s"Message was: $message")
          ExecutionFailure.noInformationFailure(message)
        }
        exception.setStackTrace(mustBeJsArray((node \ ExceptionSerializer.STACKTRACE).getOrElse(JsArray(Seq())))(readStackTrace))
        exception
      case _ => throw new IllegalArgumentException("Neither JsNull nor JsObject was found, representing an Exception.")
    }
  }

  private def getExceptionCauseOption(node: JsValue): Option[ExecutionFailure] = {
    (node \ ExceptionSerializer.CAUSE).toOption match {
      case Some(c) => Some(readException(c))
      case None => None
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

  override def write(ef: ExecutionFailure)(implicit writeContext: WriteContext[JsValue]): JsValue = writeException(ef)

  private def writeException(ef: ExecutionFailure): JsObject = {
    JsObject(Seq(
        ExceptionSerializer.CLASSNAME -> {if(ef.getExceptionClass.isEmpty) JsNull else JsString(ef.getExceptionClass)},
        ExceptionSerializer.MESSAGE -> JsString(ef.getMessage),
        ExceptionSerializer.CAUSE -> {if(ef.getCause.isEmpty) JsNull else writeException(ef.getCause.get)},
        ExceptionSerializer.STACKTRACE -> writeStackTrace(ef)
      )
    )
  }

  private def writeStackTrace(ef: ExecutionFailure): JsArray = {
    val arr = for (ste <- ef.getStackTrace) yield {
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

