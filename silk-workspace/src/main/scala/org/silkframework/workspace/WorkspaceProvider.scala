package org.silkframework.workspace

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.ResourceManager
import org.silkframework.util.Identifier

import scala.reflect.ClassTag
import scala.util.Try

trait WorkspaceProvider {

  /**
   * Reads all projects from the workspace.
   */
  def readProjects()(implicit user: UserContext): Seq[ProjectConfig]

  /**
   * Adds/Updates a project.
   */
  def putProject(projectConfig: ProjectConfig)(implicit user: UserContext): Unit

  /**
   * Deletes a project.
   */
  def deleteProject(name: Identifier)(implicit user: UserContext): Unit

  /**
   * Retrieves the project cache folder.
   */
  def projectCache(name: Identifier): ResourceManager

  /**
   * Reads all tasks of a specific type from a project.
   */
  def readTasks[T <: TaskSpec : ClassTag](project: Identifier, projectResources: ResourceManager)
                                         (implicit user: UserContext): Seq[Task[T]] = {
    readTasksSafe[T](project, projectResources).map(_.get)
  }

  /**
    * Version of readTasks that returns a Seq[Try[Task[T]]]
    **/
  def readTasksSafe[T <: TaskSpec : ClassTag](project: Identifier, projectResources: ResourceManager)(implicit user: UserContext): Seq[Try[Task[T]]]

  /**
   * Adds/Updates a task in a project.
   */
  def putTask[T <: TaskSpec : ClassTag](project: Identifier, task: Task[T])(implicit user: UserContext): Unit

  /**
   * Deletes a task from a project.
   */
  def deleteTask[T <: TaskSpec : ClassTag](project: Identifier, task: Identifier)(implicit user: UserContext): Unit
}
