package de.fuberlin.wiwiss.silk.workspace.modules

import java.util.logging.{Level, Logger}

import de.fuberlin.wiwiss.silk.runtime.activity.{Status, StatusHolder}
import de.fuberlin.wiwiss.silk.workspace.Project

import scala.xml.Node

/**
 * Base class of a cache.
 *
 * @tparam TaskType The task type for which values are cached.
 * @tparam T The type of the values that are cached.
 */
abstract class Cache[TaskType, T](initialValue: T) {

  /** The current value of this thread. */
  @volatile
  private var currentValue = initialValue

  /** Indicates if this cache has been loaded successfully */
  @volatile
  private var loaded = false

  /** The thread used to load the current value. May be None if no thread has been created yet. */
  @volatile private var loadingThread: Option[Thread] = None

  private val log = Logger.getLogger(getClass.getName)

  val status = new StatusHolder()

  /** Retrieves the current value of this cache */
  def value = currentValue

  /** Updates the current value of this cache */
  protected def value_=(newValue: T) {
    currentValue = newValue
  }

  /** Resets the cache to its initial value. Stops any running cache loading. */
  def clear() {
    //Stop loading
    for(thread <- loadingThread) {
      thread.interrupt()
      thread.join()
    }
    loadingThread = None

    //Reset value
    loaded = false
    currentValue = initialValue
  }

  /** Start loading this cache. */
  def load(project: Project, task: TaskType, updateCache: Boolean = true) {
    //Stop current loading thread
    for(thread <- loadingThread) {
      thread.interrupt()
      thread.join()
    }

    if(updateCache || !loaded) {
      //Set the task status
      status.update(Status.Started("Loading cache"))
      //Create new loading thread
      loadingThread = Some(new LoadingThread(project, task))
      //Start loading thread
      loadingThread.map(_.start())
    }
  }

  /** Blocks until this cache has been loaded */
  def waitUntilLoaded() {
    for(thread <- loadingThread) {
      thread.join()
    }
  }

  /** Writes the current value of this cache to an XML node. */
  final def toXML: Node = {
    <Cache loaded={loaded.toString}>
      { serialize }
    </Cache>
  }

  /** Reads the cache value from an XML node and updates the current value of this cache. */
  final def loadFromXML(node: Node) = {
    loaded = (node \ "@loaded").head.text.toBoolean
    deserialize((node \ "_").head)
  }

  /** Writes the current value of this cache to an XML node. */
  protected def serialize: Node

  /** Reads the cache value from an XML node and updates the current value of this cache. */
  protected def deserialize(node: Node)

  /**
   * Overridden in sub classes to do the actual loading of the cache value
   * @return True, if the cached value has been updated. False, otherwise.
   */
  protected def update(project: Project, task: TaskType): Boolean

  /** The thread that is used to load the cache value. */
  private class LoadingThread(project: Project, task: TaskType) extends Thread {
    override def run() {
      val startTime = System.currentTimeMillis
      try {
        val updated = update(project, task)
        loaded = true
        status.update(Status.Finished("Loading cache", success = true, System.currentTimeMillis - startTime, None))
        if(updated) log.info("Cache updated")
        // Commit to the project
//        if(updated && !isInterrupted) {
        // TODO Schedule write
//        }
      } catch {
        case ex: InterruptedException =>
          log.log(Level.WARNING, "Loading cache stopped")
          status.update(Status.Finished("Loading stopped", success = false, System.currentTimeMillis - startTime, None))
        case ex: Exception =>
          log.log(Level.WARNING, "Loading cache failed", ex)
          status.update(Status.Finished("Loading cache", success = false, System.currentTimeMillis - startTime, Some(ex)))
      }
      loadingThread = None
    }
  }
}