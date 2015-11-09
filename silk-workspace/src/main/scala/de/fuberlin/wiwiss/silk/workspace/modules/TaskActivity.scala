package de.fuberlin.wiwiss.silk.workspace.modules

import java.util.logging.{Level, Logger}
import de.fuberlin.wiwiss.silk.runtime.activity.{Activity, ActivityContext}
import de.fuberlin.wiwiss.silk.runtime.resource.{ResourceNotFoundException, ResourceManager}
import de.fuberlin.wiwiss.silk.runtime.serialization.XmlFormat
import de.fuberlin.wiwiss.silk.util.StringUtils._
import de.fuberlin.wiwiss.silk.runtime.serialization.Serialization._
import de.fuberlin.wiwiss.silk.util.XMLUtils._
import scala.reflect.ClassTag
import scala.xml.XML

/**
 * An activity that belongs to a task.
 *
 * @tparam A The type of the underlying activity.
 * @tparam T The type of value that is generated.
 *           Set to [[Unit]] if no values are generated.
 */
abstract class TaskActivity[A <: Activity[T] : ClassTag, T] extends Activity[T] {

  /**
   * If true, this activity is run on every update of the task it belongs to.
   */
  def autoRun: Boolean

  def activityType = implicitly[ClassTag[A]].runtimeClass

  override def name = activityType.getSimpleName.undoCamelCase

}

object TaskActivity {

  /**
   * Creates a task activity that runs in the background and does not hold a value
   *
   * @param create Given the previous value, a function to create a new activity that updates the value.
   * @return The task activity.
   */
  def apply[A <: Activity[Unit] : ClassTag](create: => A): TaskActivity[A, Unit] = new BackgroundActivity[A, Unit](Unit, (Unit) => create)

  /**
   * Creates a task activity that updates a value.
   *
   * @param initialValue The initial value.
   * @param create Given the previous value, a function to create a new activity that updates the value.
   * @tparam A The type of the activity.
   * @tparam T The type of the value.
   * @return The task activity.
   */
  def apply[A <: Activity[T] : ClassTag, T](initialValue: T, create: T => A): TaskActivity[A,T] = new BackgroundActivity[A,T](initialValue, create)

  /**
   * Creates a task activity that updates a value that is cached on the filesystem.
   *
   * @param resourceName The resource name used for persisting the value.
   * @param initialValue The initial value.
   * @param create Given the previous value, a function to create a new activity that updates the value.
   * @param resourceMgr The resource manager used for persisting the value.
   * @tparam A The type of the activity.
   * @tparam T The type of the value.
   * @return The task activity.
   */
  def apply[A <: Activity[T] : ClassTag, T](resourceName: String, initialValue: T, create: () => A, resourceMgr: ResourceManager)(implicit xmlFormat: XmlFormat[T]) : TaskActivity[A ,T] = new CachedActivity[A, T](resourceName, initialValue, create, resourceMgr)

  /**
   * A task activity that executes in the background.
   */
  private class BackgroundActivity[A <: Activity[T] : ClassTag, T](initial: T, create: T => A) extends TaskActivity[A, T] {

    override def autoRun = false

    override def initialValue = Some(initial)

    /**
     * Executes this activity.
     *
     * @param context Holds the context in which the activity is executed.
     */
    override def run(context: ActivityContext[A#ValueType]): Unit = {
      val activity = create(context.value())
      activity.run(context)
    }
  }

  /**
   * A task activity that executes in the background and caches its value.
   */
  private class CachedActivity[A <: Activity[T] : ClassTag, T](resourceName: String, initial: T, create: () => A, resourceMgr: ResourceManager)(implicit xmlFormat: XmlFormat[T]) extends TaskActivity[A, T] {

    private val log = Logger.getLogger(classOf[CachedActivity[_,_]].getName)

    @volatile
    private var initialized = false

    override def autoRun = true

    override def initialValue = Some(initial)

    override def run(context: ActivityContext[T]): Unit = {
      if(!initialized) {
        initialized = true
        readValue() match {
          case Some(value) => context.value() = value
          case None => update(context)
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
      // Run activity
      val activity = create()
      activity.run(context)
      // Persist value (if updated)
      if (updated)
        writeValue(context.value())
    }

    private def readValue(): Option[T] = {
      try {
        val xml = XML.load(resourceMgr.get(resourceName).load)
        val value = fromXml[T](xml)
        log.info(s"Cache read from $resourceName")
        Some(value)
      } catch {
        case ex: ResourceNotFoundException =>
          log.log(Level.INFO, s"Cache $resourceName not found")
          None
        case ex: Exception =>
          log.log(Level.WARNING, s"Loading cache from $resourceName failed", ex)
          None
      }
    }

    private def writeValue(value: T): Unit = {
      try {
        resourceMgr.get(resourceName).write(w => toXml[T](value).write(w))
        log.info(s"Cache written to $resourceName.")
      } catch {
        case ex: Exception =>
          log.log(Level.WARNING, s"Could not write cache to $resourceName", ex)
      }
    }
  }

}