package de.fuberlin.wiwiss.silk.workspace.modules

import de.fuberlin.wiwiss.silk.runtime.resource.{ResourceLoader, ResourceManager}
import de.fuberlin.wiwiss.silk.util.Identifier
import de.fuberlin.wiwiss.silk.workspace.Project

/**
 * A plugin that adds a new module to the workspace.
 */
trait ModulePlugin[TaskType <: ModuleTask] {

  /**
   * A prefix that uniquely identifies this module.
   */
  def prefix: String

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
