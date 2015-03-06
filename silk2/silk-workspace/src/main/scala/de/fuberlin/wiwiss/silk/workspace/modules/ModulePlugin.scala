package de.fuberlin.wiwiss.silk.workspace.modules

import de.fuberlin.wiwiss.silk.runtime.resource.{ResourceLoader, ResourceManager}
import de.fuberlin.wiwiss.silk.util.Identifier
import de.fuberlin.wiwiss.silk.workspace.Project

import scala.xml.Node

/**
 * A plugin that adds a new module to the workspace.
 */
trait ModulePlugin[DataType] {

  /**
   * A prefix that uniquely identifies this module.
   */
  def prefix: String

  def createTask(name: Identifier, taskData: DataType, project: Project): Task[DataType]

  /**
   * Loads all tasks of this module.
   */
  def loadTasks(resources: ResourceLoader, project: Project): Seq[Task[DataType]]

  /**
   * Removes a specific task.
   */
  def removeTask(taskId: Identifier, resources: ResourceManager)

  /**
   * Writes an updated task.
   */
  def writeTask(task: Task[DataType], resources: ResourceManager)
}
