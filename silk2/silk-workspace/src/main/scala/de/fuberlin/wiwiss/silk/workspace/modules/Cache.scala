package de.fuberlin.wiwiss.silk.workspace.modules

import de.fuberlin.wiwiss.silk.workspace.Project
import de.fuberlin.wiwiss.silk.util.task.{TaskFinished, TaskStarted, HasStatus}
import java.util.logging.Level
import xml.{Node, NodeSeq}
import de.fuberlin.wiwiss.silk.workspace.modules.linking.LinkingTask
import de.fuberlin.wiwiss.silk.workspace.modules.source.SourceTask

/**
 * Base trait of a cache.
 *
 * @tparam TaskType The task type for which values are cached.
 * @tparam T The type of the values that are cached.
 */
abstract class Cache[TaskType <: ModuleTask, T <: AnyRef](initialValue: T) extends HasStatus {

  /** The current value of this thread. */
  private var currentValue = initialValue

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
    currentValue = initialValue
  }

  /** Start loading this cache. */
  def load(project: Project, task: TaskType) {
    //Stop current loading thread
    for(thread <- loadingThread) {
      thread.interrupt()
      thread.join()
    }

    //Set the task status
    updateStatus(TaskStarted("Loading cache"))

    //Create new loading thread
    loadingThread = Some(new LoadingThread(project, task))

    //Start loading thread
    loadingThread.map(_.start())
  }

  /** Blocks until this cache has been loaded */
  def waitUntilLoaded() {
    for(thread <- loadingThread) {
      thread.join()
    }
  }

  /** Writes the current value of this cache to an XML node. */
  def toXML: NodeSeq

  /** Reads the cache value from an XML node and updates the current value of this cache. */
  def loadFromXML(node: Node)

  /** Overridden in sub classes to do the actual loading of the cache value. */
  protected def update(project: Project, task: TaskType)

  /** The thread that is used to load the cache value. */
  private class LoadingThread(project: Project, task: TaskType) extends Thread {
    override def run() {
      val startTime = System.currentTimeMillis
      try {
        update(project, task)
        updateStatus(TaskFinished("Loading cache", true, System.currentTimeMillis - startTime, None))
        // Commit to the project
        //TODO  Make project modules (e.g. the linking module) register a callback
        if(!isInterrupted) {
          task match {
            case linkingTask: LinkingTask => project.linkingModule.update(linkingTask)
            case sourceTask: SourceTask => project.sourceModule.update(sourceTask)
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
