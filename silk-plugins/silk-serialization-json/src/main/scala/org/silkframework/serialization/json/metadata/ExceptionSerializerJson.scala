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
        val messageOpt: Option[String] = Try (stringValue(node, ExceptionSerializer.MESSAGE)).toOption
        val exceptionClassOpt: Option[Class[Throwable]] = getExceptionClassOption(className)
        val exceptionCauseOpt: Option[Throwable] = getExceptionCauseOption(node)
        val constructorAndArguments: Option[(Constructor[Throwable], Seq[Object])] = getExceptionConstructorOption(
          exceptionCauseOpt, exceptionClassOpt, messageOpt, className
        )

        val exception = if (constructorAndArguments.nonEmpty) {
          val constructorAndArguments = getExceptionConstructorOption(exceptionCauseOpt, exceptionClassOpt, messageOpt, className)
          constructorAndArguments.get._1.newInstance(constructorAndArguments.get._2: _*)
        } else {
          new Exception(
            s"Emulated Exception of class: $className original message: ${messageOpt.orNull}",
            exceptionCauseOpt.orNull
          )
        }
        exception.setStackTrace(mustBeJsArray((node \ ExceptionSerializer.STACKTRACE).getOrElse(JsArray(Seq())))(readStackTrace))
        exception

      case _ => throw new IllegalArgumentException("Neither JsNull nor JsObject was found, representing an Exception.")
    }
  }

  /**
    * Determines the constructor of a Throwable and its arguments as an object set.
    * Depending on the input, different constructors a searched via reflection.
    * A (String, Throwable) constructor is preferred, after that (Throwable, String), (Throwable), (String)
    * and finally a no argument constructor.
    *
    * @param cause            Optional Throwable
    * @param exceptionClass   Optional Class
    * @param message          Message
    * @param className        className
    * @return Constructor of a Throwable and its parameters or None
    */
  private def getExceptionConstructorOption(cause: Option[Throwable], exceptionClass: Option[Class[Throwable]],
                                            message: Option[String], className: String): Option[(Constructor[Throwable], Seq[Object])] = {
    try {
      val candidates = exceptionClass.get.getConstructors.filter(
        c => c.getParameterTypes.contains(classOf[String]) || c.getParameterTypes.contains(classOf[Throwable])
      ).sortWith( (c1,c2) => c1.getParameterCount > c2.getParameterCount)

      candidates.map(_.getParameterTypes).head match {
        case Array(_, _) =>
          try {
            val constructor: Constructor[Throwable] = exceptionClass.get.getConstructor(classOf[String], classOf[Throwable])
            val args: Seq[Object] = Seq(message.getOrElse("null"), cause.orNull)
            Some((constructor, args))
          }
          catch {
            case _: NoSuchElementException =>
              val constructor: Constructor[Throwable] = exceptionClass.get.getConstructor(classOf[Throwable], classOf[String])
              val args: Seq[Object]  = Seq(cause.orNull, message.getOrElse("null"))
              Some((constructor, args))
            case _: Throwable =>
              None
          }
        case Array(_) =>
          try {
            val constructor: Constructor[Throwable] = exceptionClass.get.getConstructor(classOf[Throwable])
            val args = Seq(cause.orNull)
            Some((constructor, args))
          }
          catch {
            case _: NoSuchElementException =>
              val constructor: Constructor[Throwable] = exceptionClass.get.getConstructor(classOf[String])
              val args = Seq(message.getOrElse("null"))
              Some(constructor, args)
            case _: Throwable => None
          }
        case Array() =>
          val constructor: Constructor[Throwable] = exceptionClass.get.getConstructor()
          val args = Seq()
          Some(constructor, args)
        case _ =>
          None
      }
    }
    catch {
      case _: java.lang.IllegalArgumentException =>
        logger.warn(s"A constructor for the exception: $className could not be found because of the given arguments")
        None
      case _: Throwable =>
        throw new RuntimeException(s"Construction of exception $className failed for unknown reasons")
    }
  }

  /* Following methods help to get optional classes, causes etc. to make the above readable and usable */
  private def getExceptionClassOption(className: String): Option[Class[Throwable]] = {
    try {
      Some(Class.forName(className).asInstanceOf[Class[Throwable]])
    }
    catch {
      case _: Throwable =>
        logger.error(s"The raised exception object $className does not exist as a known class and can't be serialized")
        None
    }
  }

  private def getExceptionCauseOption(node: JsValue): Option[Throwable] = {
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
      val lineNumber = numberValue(ste, ExceptionSerializer.LINENUMBER)
      new StackTraceElement(className, methodName, fileName, if (lineNumber != null) lineNumber.toInt else -1)
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

