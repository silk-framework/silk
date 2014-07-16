package de.fuberlin.wiwiss.silk.workspace.modules

import java.util.logging.{Level, Logger}

import de.fuberlin.wiwiss.silk.runtime.resource.ResourceManager
import de.fuberlin.wiwiss.silk.util.{Identifier, Timer}
import de.fuberlin.wiwiss.silk.workspace.Project

class Module[ConfigType <: ModuleConfig, TaskType <: ModuleTask](provider: ModuleProvider[ConfigType, TaskType], resourceMgr: ResourceManager, project: Project) {

  /* Do not write more frequently than this (in milliseconds) */
  private val writeInterval = 5000L

  private val logger = Logger.getLogger(classOf[Module[_, _]].getName)

  /**
   * Cache all tasks of this module in memory.
   */
  @volatile
  private var cachedTasks : Map[Identifier, TaskType] = {
    val tasks = provider.loadTasks(resourceMgr, project)
    tasks.map(task => (task.name, task)).toMap
  }

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

  /**
   * Retrieves the configuration of this module.
   */
  def config = provider.loadConfig(resourceMgr)

  /**
   * Updates the configuration of this module.
   */
  def config_=(c : ConfigType) { provider.writeConfig(config, resourceMgr) }

  /**
   * Retrieves all tasks in this module.
   */
  def tasks: Seq[TaskType] = {
    cachedTasks.values.toSeq
  }

  /**
   * Retrieves a task by name.
   *
   * @throws java.util.NoSuchElementException If no task with the given name has been found
   */
  def task(name: Identifier): TaskType = {
    cachedTasks.getOrElse(name, throw new NoSuchElementException(s"Task '$name' not found in ${getClass.getSimpleName}"))
  }

  def taskOption(name: Identifier): Option[TaskType] = {
    cachedTasks.get(name)
  }

  /**
   * Updates a specific task.
   */
  def update(task : TaskType) {
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