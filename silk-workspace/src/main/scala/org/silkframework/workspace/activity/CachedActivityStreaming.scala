package org.silkframework.workspace.activity

import java.io.BufferedInputStream
import java.util.logging.Level

import org.silkframework.runtime.activity.ActivityContext
import org.silkframework.runtime.resource.ResourceNotFoundException
import org.silkframework.runtime.serialization.{ReadContext, StreamXml, StreamXmlFormat}

import scala.util.Try
import scala.util.control.NonFatal

/**
  * A cached activity that reads and writes the serialized XML via streaming to reduce the memory footprint
  */
trait CachedActivityStreaming[T] extends CachedActivity[T] {
  // Implicit parameters for traits solution from https://stackoverflow.com/questions/6983759/how-to-declare-traits-as-taking-implicit-constructor-parameters
  protected case class WrappedStreamXmlFormat(implicit val wrapped: StreamXmlFormat[T])

  override protected lazy val wrappedXmlFormat: WrappedXmlFormat = {
    throw new RuntimeException("wrappedXmlFormat should not be accessed in a class implementing the CachedActivityStreaming trait")
  }

  // normally defined by caller as val wrappedXmlFormat = WrappedXmlFormat()
  protected val wrappedStreamXmlFormat: WrappedStreamXmlFormat

  import wrappedStreamXmlFormat.wrapped

  override protected def readValue(context: ActivityContext[T]): Option[T] = {
    try {
      implicit val readContext: ReadContext = ReadContext()
      val inputStream = new BufferedInputStream(resource.inputStream)
      val value = StreamXml.read[T](inputStream)
      context.log.info(s"Cache read from $resource")
      Try(inputStream.close())
      Some(value)
    } catch {
      case _: ResourceNotFoundException =>
        context.log.log(Level.INFO, s"No existing cache found at $resource. Loading cache...")
        None
      case NonFatal(ex) =>
        context.log.log(Level.WARNING, s"Loading cache from $resource failed", ex)
        None
    }
  }

  override protected def writeValue(context: ActivityContext[T]): Unit = {
    try {
      resource.write() { outputStream =>
        StreamXml.write[T](context.value(), outputStream)
        context.log.info(s"Cache written to $resource.")
      }
    } catch {
      case NonFatal(ex) =>
        context.log.log(Level.WARNING, s"Could not write cache to $resource", ex)
    }
  }
}
