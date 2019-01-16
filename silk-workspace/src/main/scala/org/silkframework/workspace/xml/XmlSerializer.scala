package org.silkframework.workspace.xml

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.runtime.resource.{ResourceLoader, ResourceManager}
import org.silkframework.util.Identifier

import scala.util.Try

/**
 * A plugin that adds a new module to the workspace.
 */
private trait XmlSerializer[TaskType <: TaskSpec] {

  /**
   * A prefix that uniquely identifies this module.
   */
  def prefix: String

  /**
   * Loads all tasks of this module.
   */
  def loadTasks(resources: ResourceLoader, projectResources: ResourceManager): Seq[Task[TaskType]] = {
    loadTasksSafe(resources, projectResources).map(_.get)
  }

  /**
    * Loads all tasks of this module in a safe way. Invalid tasks can be handled separately this way.
    */
  def loadTasksSafe(resources: ResourceLoader, projectResources: ResourceManager): Seq[Try[Task[TaskType]]]

  /**
   * Removes a specific task.
   */
  def removeTask(name: Identifier, resources: ResourceManager)

  /**
   * Writes an updated task.
   */
  def writeTask(task: Task[TaskType], resources: ResourceManager)
}
