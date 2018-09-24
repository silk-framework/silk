package org.silkframework.workspace.activity

import java.util.logging.Level

import org.silkframework.runtime.activity.ActivityContext
import org.silkframework.runtime.resource.ResourceNotFoundException
import org.silkframework.runtime.serialization.{ReadContext, StreamXml}

import scala.util.Try
import scala.util.control.NonFatal

/**
  * A cached activity that reads and writes the serialized XML via streaming to reduce the memory footprint
  */
trait CachedActivityStreaming[T] extends CachedActivity[T] {
  override protected def readValue(context: ActivityContext[T]): Option[T] = {
    try {
      implicit val readContext: ReadContext = ReadContext()
      val inputStream = resource.inputStream
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
