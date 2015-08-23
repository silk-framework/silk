package de.fuberlin.wiwiss.silk.workspace.modules

import de.fuberlin.wiwiss.silk.runtime.resource.{ResourceLoader, ResourceManager}
import de.fuberlin.wiwiss.silk.util.Identifier
import de.fuberlin.wiwiss.silk.workspace.Project

/**
 * A plugin that adds a new module to the workspace.
 */
trait ModulePlugin[DataType] {

  /**
   * A prefix that uniquely identifies this module.
   */
  def prefix: String

  /**
   * Loads all tasks of this module.
   */
  def loadTasks(resources: ResourceLoader, projectResources: ResourceLoader): Map[Identifier, DataType]

  /**
   * Removes a specific task.
   */
  def removeTask(name: Identifier, resources: ResourceManager)

  /**
   * Writes an updated task.
   */
  def writeTask(data: DataType, resources: ResourceManager)

  /**
   * The activities that belong to a given task.
   */
  def activities(task: Task[DataType], project: Project): Seq[TaskActivity[_,_]] = Seq.empty
}
