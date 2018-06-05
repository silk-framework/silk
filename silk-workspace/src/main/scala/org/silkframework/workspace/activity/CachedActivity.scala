package org.silkframework.workspace.activity

import java.util.logging.Level

import org.silkframework.runtime.activity.{Activity, ActivityContext, ActivityControl}
import org.silkframework.runtime.resource.{ResourceNotFoundException, WritableResource}
import org.silkframework.runtime.serialization.{ReadContext, XmlFormat}
import org.silkframework.runtime.serialization.XmlSerialization._
import org.silkframework.util.XMLUtils._

import scala.util.control.NonFatal
import scala.xml.XML

/**
  * Caches the value of an activity on the filesystem.
  *
  * @tparam T The type of the value.
  */
trait CachedActivity[T] extends Activity[T] {

  /** The resource used for persisting the value. */
  def resource: WritableResource

  // Implicit parameters for traits solution from https://stackoverflow.com/questions/6983759/how-to-declare-traits-as-taking-implicit-constructor-parameters
  protected case class WrappedXmlFormat(implicit val wrapped: XmlFormat[T])

  // normally defined by caller as val wrappedXmlFormat = WrappedXmlFormat()
  protected val wrappedXmlFormat: WrappedXmlFormat

  // will have to repeat this when you extend T and need access to the implicit
  import wrappedXmlFormat.wrapped

  @volatile
  private var initialized = false

  // Externally set to mark this cache as dirty, e.g. by observing the source tasks for changes
  @volatile
  var dirty: Boolean = false

  override def run(context: ActivityContext[T]): Unit = {
    var currentDirty = true
    while(currentDirty) {
      dirty = false
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
      currentDirty = dirty // dirty flag may have changed in the meantime
    }
  }

  private def update(context: ActivityContext[T]) = {
    // Listen for value updates
    var updated = false
    val updateFunc = (value: T) => { updated = true }
    context.value.subscribe(updateFunc)
    // Update cache
    this.run(context)
    // Persist value (if updated)
    if (updated)
      writeValue(context)
  }

  private def readValue(context: ActivityContext[T]): Option[T] = {
    try {
      val xml = resource.read(XML.load)
      implicit val readContext = ReadContext()
      val value = fromXml[T](xml)
      context.log.info(s"Cache read from $resource")
      Some(value)
    } catch {
      case ex: ResourceNotFoundException =>
        context.log.log(Level.INFO, s"No existing cache found at $resource. Loading cache...")
        None
      case NonFatal(ex) =>
        context.log.log(Level.WARNING, s"Loading cache from $resource failed", ex)
        None
    }
  }

  private def writeValue(context: ActivityContext[T]): Unit = {
    try {
      resource.write()(w => toXml[T](context.value()).write(w))
      context.log.info(s"Cache written to $resource.")
    } catch {
      case NonFatal(ex) =>
        context.log.log(Level.WARNING, s"Could not write cache to $resource", ex)
    }
  }

  def startDirty(taskActivity: ActivityControl[_]): Unit = {
    dirty = true

    if(taskActivity.status().isRunning) {
      // Do nothing, the dirty flag should be picked up by the activity execution
    } else{
      try {
        taskActivity.start()
      } catch {
        case _: IllegalStateException =>
        // Ignore exception because of race condition that another thread already started the activity
      }
    }
  }
}
