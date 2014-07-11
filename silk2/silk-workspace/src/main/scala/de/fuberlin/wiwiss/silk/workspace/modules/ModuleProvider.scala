package de.fuberlin.wiwiss.silk.workspace.modules

import de.fuberlin.wiwiss.silk.runtime.resource.{ResourceLoader, ResourceManager}
import de.fuberlin.wiwiss.silk.util.Identifier
import de.fuberlin.wiwiss.silk.workspace.Project

/**
 * Overwritten by module implementations.
 */
trait ModuleProvider[ConfigType <: ModuleConfig, TaskType <: ModuleTask] {

  /**
   * Loads the configuration for this module.
   */
  def loadConfig(resources: ResourceLoader): ConfigType

  /**
   * Writes updated configuration for this module.
   */
  def writeConfig(config: ConfigType, resources: ResourceManager)

  /**
   * Loads all tasks of this module.
   */
  def loadTasks(resources: ResourceLoader, project: Project): Seq[TaskType]

  /**
   * Removes a specific task.
   */
  def removeTask(taskId: Identifier, resources: ResourceManager)

  /**
   * Writes an updated task.
   */
  def writeTask(task: TaskType, resources: ResourceManager)
}
