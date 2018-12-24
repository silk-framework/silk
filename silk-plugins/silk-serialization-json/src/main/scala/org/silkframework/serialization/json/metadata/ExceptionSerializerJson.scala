package org.silkframework.serialization.json.metadata

import java.lang.reflect.Constructor

import org.silkframework.entity.metadata.{EntityMetadata, ExceptionSerializer}
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.serialization.json.JsonHelpers._
import org.slf4j.LoggerFactory
import play.api.libs.json._

import scala.util.Try

case class ExceptionSerializerJson() extends JsonMetadataSerializer[Throwable] {

  @transient
  private val logger = LoggerFactory.getLogger(this.getClass)

  override def read(ex: JsValue)(implicit readContext: ReadContext): Throwable = readException(ex)

  def readException(node: JsValue): Throwable = {
    node match {
      case JsNull => null
      case JsObject(_) =>
        val className: String = stringValue(node, ExceptionSerializer.CLASS)
        // FIXME: Many assumptions here, i was instructed to not use null, so guess i need to change the whole method, Really, that might need to redesigned anyway,
        val messageOpt: Option[String] = Try (stringValue(node, ExceptionSerializer.MESSAGE)).toOption
        val exceptionClassOpt: Option[Class[Throwable]] = getExceptionClass(className)
        val exceptionCauseOpt: Option[Throwable] = getExceptionCause(node)
        val constructorOpt: Option[Constructor[Throwable]] = getExceptionConstructor(
          exceptionCauseOpt, exceptionClassOpt, messageOpt, className
        )
        if (constructorOpt.nonEmpty && exceptionCauseOpt.isEmpty) {
          val exception = exceptionClassOpt.get.cast(constructorOpt.get.newInstance(Seq[Object](): _*))
          exception.setStackTrace(mustBeJsArray((node \ ExceptionSerializer.STACKTRACE).getOrElse(JsArray(Seq())))(readStackTrace))
          exception
        }
        else if (constructorOpt.nonEmpty && exceptionCauseOpt.nonEmpty) {
          val exception = exceptionClassOpt.get.cast(constructorOpt.get.newInstance(exceptionCauseOpt.get, messageOpt.orNull))
          exception.setStackTrace(mustBeJsArray((node \ ExceptionSerializer.STACKTRACE).getOrElse(JsArray(Seq())))(readStackTrace))
          exception
        }
        else {
          new Exception(
            "Emulated Exception of class: $className original message: " +  messageOpt.orNull,
            exceptionCauseOpt.getOrElse(UnknownCauseException("Exception with an unknown source was serialized b/c missing exception information"))
          )
        }
      case _ => throw new IllegalArgumentException("Neither JsNull nor JsObject was found, representing an Exception.")
    }
  }


  /**
    * Some method to determine the constructor of a Throwable with out that being necessarily there.
    * Depending the input, that is largely optional diffrent constructors a searched via reflection.
    *
    * @param cause            Optional Throwable
    * @param exceptionClass   Optional Class
    * @param message          Message
    * @param className        className
    * @return Constructor of a Throwable or None
    */
  private def getExceptionConstructor(cause: Option[Throwable], exceptionClass: Option[Class[Throwable]],
                               message: Option[String], className: String): Option[Constructor[Throwable]] = {
    try {
      var arguments = Seq[Object]()
      if (cause.nonEmpty && exceptionClass.nonEmpty) {
        var constructor: Constructor[Throwable] = exceptionClass.get.getConstructor(classOf[String], classOf[Throwable])
        arguments = Seq(message.orNull, cause.get)
        if (constructor == null) {
          constructor = exceptionClass.get.getConstructor(classOf[Throwable], classOf[String])
        }
        Some(constructor)
      }
      else {
        if (cause.nonEmpty) {
          arguments = Seq(message.orNull)
          Some(exceptionClass.get.getConstructor(classOf[String]))
        }
        else {
          None // no class -> no constructor
        }
      }
    }
    catch {
      case _: java.lang.NoSuchMethodException =>
        logger.error(s"A constructor for the exception: $className could not be found and can not be serialized")
        None // no constructor
      case _: Throwable =>
        throw new RuntimeException(s"Construction of exception class $className failed for unknown reasons")
    }
  }

  /* Following methods help to get optional classes, causes etc. to make the above readable and usable */
  private def getExceptionClass(className: String):Option[Class[Throwable]] = {
    try {
      Some(Class.forName(className).asInstanceOf[Class[Throwable]])
    } catch {
      case _: Throwable =>
        logger.error("The raised exception does not exist as a known class and can't be serialized")
        None
    }
  }

  private def getExceptionCause(node: JsValue): Option[Throwable] = {
    (node \ ExceptionSerializer.CAUSE).toOption match {
      case Some(c) => Some(readException(c))
      case None => None
    }
  }

  def readStackTrace(node: JsArray): Array[StackTraceElement] = {
    val stackTrace = for (ste <- node.value) yield {

      val className = stringValue(ste, ExceptionSerializer.CLASSNAME)
      val methodName = stringValue(ste, ExceptionSerializer.METHODNAME)
      val fileName: String = stringValue(ste, ExceptionSerializer.FILENAME)
      //stringValueOption(ste, ExceptionSerializer.FILENAME).getOrElse("unknown")
      val lineNumber = numberValue(ste, ExceptionSerializer.LINENUMBER)
      new StackTraceElement(className, methodName, fileName, if (lineNumber != null) lineNumber.toInt else 0)
    }

    stackTrace.toArray
  }

  override def write(ex: Throwable)(implicit writeContext: WriteContext[JsValue]): JsValue = writeException(ex)

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

  private def writeException(ex: Throwable): JsObject = {
    JsObject(
      Seq(
        ExceptionSerializer.CLASS -> JsString(ex.getClass.getCanonicalName),
        ExceptionSerializer.MESSAGE -> JsString(ex.getMessage),
        ExceptionSerializer.CAUSE -> (if (ex.getCause != null) writeException(ex.getCause) else JsNull),
        ExceptionSerializer.STACKTRACE -> writeStackTrace(ex)
      )
    )
  }

  private def writeStackTrace(ex: Throwable): JsArray = {
    val arr = for (ste <- ex.getStackTrace) yield {
      JsObject(Seq(
        ExceptionSerializer.FILENAME -> JsString(ste.getFileName),
        ExceptionSerializer.CLASSNAME -> JsString(ste.getClassName),
        ExceptionSerializer.METHODNAME -> JsString(ste.getMethodName),
        ExceptionSerializer.LINENUMBER -> JsNumber(ste.getLineNumber)
      ))
    }
    JsArray(arr)
  }
}

