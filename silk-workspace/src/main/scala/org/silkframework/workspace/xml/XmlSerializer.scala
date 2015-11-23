package org.silkframework.workspace.xml

import org.silkframework.runtime.resource.{ResourceLoader, ResourceManager}
import org.silkframework.util.Identifier

/**
 * A plugin that adds a new module to the workspace.
 */
private trait XmlSerializer[DataType] {

  /**
   * A prefix that uniquely identifies this module.
   */
  def prefix: String

  /**
   * Loads all tasks of this module.
   */
  def loadTasks(resources: ResourceLoader, projectResources: ResourceManager): Map[Identifier, DataType]

  /**
   * Removes a specific task.
   */
  def removeTask(name: Identifier, resources: ResourceManager)

  /**
   * Writes an updated task.
   */
  def writeTask(data: DataType, resources: ResourceManager)
}
