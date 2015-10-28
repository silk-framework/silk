package de.fuberlin.wiwiss.silk.workspace

import java.io.{InputStream, OutputStream}

import de.fuberlin.wiwiss.silk.runtime.resource.{ResourceManager, EmptyResourceManager, ResourceLoader}
import de.fuberlin.wiwiss.silk.util.Identifier
import scala.reflect.ClassTag

trait WorkspaceProvider {

  /**
   * Reads all projects from the workspace.
   */
  def readProjects(): Seq[ProjectConfig]

  /**
   * Adds/Updates a project.
   */
  def putProject(project: ProjectConfig): Unit

  /**
   * Deletes a project.
   */
  def deleteProject(name: Identifier): Unit

  /**
   * Retrieves the project resources (e.g. associated files).
   */
  def projectResources(name: Identifier): ResourceManager

  /**
   * Retrieves the project cache folder.
   */
  def projectCache(name: Identifier): ResourceManager

  /**
   * Reads all tasks of a specific type from a project.
   */
  def readTasks[T: ClassTag](project: Identifier): Seq[(Identifier, T)]

  /**
   * Adds/Updates a task in a project.
   */
  def putTask[T: ClassTag](project: Identifier, data: T): Unit

  /**
   * Deletes a task from a project.
   */
  def deleteTask[T: ClassTag](project: Identifier, task: Identifier): Unit

  /**
   * Exports a project to a file.
   * Returns the proposed file name.
   */
  def exportProject(project: Identifier, outputStream: OutputStream): String = {
    throw new UnsupportedOperationException("The configured workspace provider does not support exporting projects!")
  }

  /**
   * Imports a project from a file.
   */
  def importProject(project: Identifier, inputStream: InputStream, resources: ResourceLoader = EmptyResourceManager): Unit = {
    throw new UnsupportedOperationException("The configured workspace provider does not support importing projects!")
  }
}