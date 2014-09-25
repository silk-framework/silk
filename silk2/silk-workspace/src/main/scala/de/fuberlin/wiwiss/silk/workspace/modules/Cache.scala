package de.fuberlin.wiwiss.silk.workspace.modules

import java.util.logging.Level

import de.fuberlin.wiwiss.silk.runtime.task.{HasStatus, TaskFinished, TaskStarted}
import de.fuberlin.wiwiss.silk.workspace.Project
import de.fuberlin.wiwiss.silk.workspace.modules.dataset.DatasetTask
import de.fuberlin.wiwiss.silk.workspace.modules.linking.LinkingTask
import de.fuberlin.wiwiss.silk.workspace.modules.transform.TransformTask

import scala.xml.Node

/**
 * Base class of a cache.
 *
 * @tparam TaskType The task type for which values are cached.
 * @tparam T The type of the values that are cached.
 */
abstract class Cache[TaskType <: ModuleTask, T <: AnyRef](initialValue: T) extends HasStatus {

  /** The current value of this thread. */
  @volatile
  private var currentValue = initialValue

  /** Indicates if this cache has been loaded successfully */
  @volatile
  private var loaded = false

  /** The thread used to load the current value. May be None if no thread has been created yet. */
  @volatile private var loadingThread: Option[Thread] = None

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
  def load(project: Project, task: TaskType) {
    //Stop current loading thread
    for(thread <- loadingThread) {
      thread.interrupt()
      thread.join()
    }

    if(!loaded) {
      //Set the task status
      updateStatus(TaskStarted("Loading cache"))
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
    deserialize(node \ "_" head)
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
        updateStatus(TaskFinished("Loading cache", true, System.currentTimeMillis - startTime, None))
        if(updated) logger.info("Cache updated")
        // Commit to the project
        //TODO  Make project modules (e.g. the linking module) register a callback
        if(updated && !isInterrupted) {
          task match {
            case transformTask: TransformTask => project.updateTask(transformTask)
            case linkingTask: LinkingTask => project.updateTask(linkingTask)
            case datasetTask: DatasetTask => project.updateTask(datasetTask)
          }
        }
      } catch {
        case ex: InterruptedException =>
          logger.log(Level.WARNING, "Loading cache stopped")
          updateStatus(TaskFinished("Loading stopped", false, System.currentTimeMillis - startTime, None))
        case ex: Exception =>
          logger.log(Level.WARNING, "Loading cache failed", ex)
          updateStatus(TaskFinished("Loading cache", false, System.currentTimeMillis - startTime, Some(ex)))
      }
      loadingThread = None
    }
  }
}
