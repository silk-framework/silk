package org.silkframework.workspace

import org.silkframework.config.{Task, MetaData, TaskSpec}
import org.silkframework.runtime.resource.ResourceManager
import org.silkframework.util.Identifier

import scala.reflect.ClassTag

trait WorkspaceProvider {

  /**
   * Reads all projects from the workspace.
   */
  def readProjects(): Seq[ProjectConfig]

  /**
   * Adds/Updates a project.
   */
  def putProject(projectConfig: ProjectConfig): Unit

  /**
   * Deletes a project.
   */
  def deleteProject(name: Identifier): Unit

  /**
   * Retrieves the project cache folder.
   */
  def projectCache(name: Identifier): ResourceManager

  /**
   * Reads all tasks of a specific type from a project.
   */
  def readTasks[T <: TaskSpec : ClassTag](project: Identifier, projectResources: ResourceManager): Seq[Task[T]]

  /**
   * Adds/Updates a task in a project.
   */
  def putTask[T <: TaskSpec : ClassTag](project: Identifier, task: Task[T]): Unit

  /**
   * Deletes a task from a project.
   */
  def deleteTask[T <: TaskSpec : ClassTag](project: Identifier, task: Identifier): Unit
}
