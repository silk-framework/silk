package de.fuberlin.wiwiss.silk.workspace.modules

import java.util.logging.{Level, Logger}

import de.fuberlin.wiwiss.silk.runtime.resource.ResourceManager
import de.fuberlin.wiwiss.silk.util.{Identifier, Timer}
import de.fuberlin.wiwiss.silk.workspace.Project

import scala.reflect.ClassTag

class Module[TaskType <: ModuleTask : ClassTag](provider: ModuleProvider[TaskType], resourceMgr: ResourceManager, project: Project) {

  /* Do not write more frequently than this (in milliseconds) */
  private val writeInterval = 5000L

  private val logger = Logger.getLogger(classOf[Module[_]].getName)

  /**
   * Cache all tasks of this module in memory.
   */
  @volatile
  private var cachedTasks : Map[Identifier, TaskType] = null

  /**
   * Remember which tasks have been updated, but have not been written yet.
   */
  @volatile
  private var updatedTasks = Map[Identifier, TaskType]()

  /**
   * Remember the time of the last write.
   */
  @volatile
  private var lastUpdateTime = 0L

  // Start a background writing thread
  WriteThread.start()

  def loadTasks() = {
    if(cachedTasks == null) {
      cachedTasks = {
        val tasks = provider.loadTasks(resourceMgr, project)
        tasks.map(task => (task.name, task)).toMap
      }
    }
  }

  def hasTaskType[T <: ModuleTask : ClassTag]: Boolean = {
    implicitly[ClassTag[T]].runtimeClass == implicitly[ClassTag[TaskType]].runtimeClass
  }

  def taskType: String = {
    implicitly[ClassTag[TaskType]].runtimeClass.getName
  }

  /**
   * Retrieves all tasks in this module.
   */
  def tasks: Seq[TaskType] = {
    loadTasks()
    cachedTasks.values.toSeq
  }

  /**
   * Retrieves a task by name.
   *
   * @throws java.util.NoSuchElementException If no task with the given name has been found
   */
  def task(name: Identifier): TaskType = {
    loadTasks()
    cachedTasks.getOrElse(name, throw new NoSuchElementException(s"Task '$name' not found in ${project.name}"))
  }

  def taskOption(name: Identifier): Option[TaskType] = {
    loadTasks()
    cachedTasks.get(name)
  }

  /**
   * Updates a specific task.
   */
  def update(task : TaskType) {
    loadTasks()
    cachedTasks += (task.name -> task)
    updatedTasks += (task.name -> task)
    lastUpdateTime = System.currentTimeMillis

    logger.info("Updated task '" + task.name + "'")
  }

  /**
   * Removes a task from this module.
   */
  def remove(taskId : Identifier) {
    provider.removeTask(taskId, resourceMgr)

    loadTasks()
    cachedTasks -= taskId
    updatedTasks -= taskId

    logger.info("Removed task '" + taskId + "'")
  }

  /**
   * Persists a task.
   */
  private def write() {
    val tasksToWrite = updatedTasks.values.toList
    updatedTasks --= tasksToWrite.map(_.name)

    for(task <- tasksToWrite) Timer("Writing task " + task.name + " to disk") {
      provider.writeTask(task, resourceMgr)
    }
  }

  private object WriteThread extends Thread {
    override def run() {
      while(true) {
        val time = System.currentTimeMillis - lastUpdateTime

        if(updatedTasks.isEmpty) {
          Thread.sleep(writeInterval)
        }
        else if(time >= writeInterval) {
          try {
            Module.this.write()
          }
          catch {
            case ex : Exception => logger.log(Level.WARNING, "Error writing tasks", ex)
          }
        }
        else {
          Thread.sleep(writeInterval - time)
        }
      }
    }
  }
}