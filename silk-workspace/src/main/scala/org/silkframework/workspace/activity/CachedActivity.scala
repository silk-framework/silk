package org.silkframework.workspace.activity

import java.util.logging.Level

import org.silkframework.runtime.activity.{Activity, ActivityContext}
import org.silkframework.runtime.resource.{ResourceNotFoundException, WritableResource}
import org.silkframework.runtime.serialization.Serialization._
import org.silkframework.runtime.serialization.XmlFormat
import org.silkframework.util.XMLUtils._

import scala.xml.XML

/**
  * Caches the value of an activity on the filesystem.
  *
  * @param activity The activity whose value is to be cached
  * @param resource The resource used for persisting the value.
  * @tparam T The type of the value.
  */
class CachedActivity[T](activity: Activity[T], resource: WritableResource)(implicit xmlFormat: XmlFormat[T]) extends Activity[T] {

  @volatile
  private var initialized = false

  override def name = activity.name

  override def initialValue = activity.initialValue

  override def run(context: ActivityContext[T]): Unit = {
    if(!initialized) {
      initialized = true
      if(resource.exists) {
        readValue(context) match {
          case Some(value) => context.value() = value
          case None => update(context)
        }
      } else {
        context.log.log(Level.INFO, s"No existing cache found at $resource. Loading cache...")
        update(context)
      }
    } else {
      update(context)
    }
  }

  private def update(context: ActivityContext[T]) = {
    // Listen for value updates
    var updated = false
    val updateFunc = (value: T) => { updated = true }
    context.value.onUpdate(updateFunc)
    // Update cache
    activity.run(context)
    // Persist value (if updated)
    if (updated)
      writeValue(context)
  }

  private def readValue(context: ActivityContext[T]): Option[T] = {
    try {
      val xml = XML.load(resource.load)
      val value = fromXml[T](xml)
      context.log.info(s"Cache read from $resource")
      Some(value)
    } catch {
      case ex: ResourceNotFoundException =>
        context.log.log(Level.INFO, s"No existing cache found at $resource. Loading cache...")
        None
      case ex: Exception =>
        context.log.log(Level.WARNING, s"Loading cache from $resource failed", ex)
        None
    }
  }

  private def writeValue(context: ActivityContext[T]): Unit = {
    try {
      resource.write(w => toXml[T](context.value()).write(w))
      context.log.info(s"Cache written to $resource.")
    } catch {
      case ex: Exception =>
        context.log.log(Level.WARNING, s"Could not write cache to $resource", ex)
    }
  }
}
