package org.silkframework.workspace

import java.io.{InputStream, OutputStream}

import org.silkframework.runtime.resource.{EmptyResourceManager, ResourceLoader, ResourceManager}
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
  def putTask[T: ClassTag](project: Identifier, task: Identifier, data: T): Unit

  /**
   * Deletes a task from a project.
   */
  def deleteTask[T: ClassTag](project: Identifier, task: Identifier): Unit

  /**
   * Exports a project to a file.
   * Returns the proposed file name.
   */
  @deprecated(message = "Use the methods taking a marshalling object instead", since = "2.6.1")
  def exportProject(project: Identifier, outputStream: OutputStream): String = {
    throw new UnsupportedOperationException("The configured workspace provider does not support exporting projects!")
  }

  /**
   * Imports a project from a file.
   */
  @deprecated(message = "Use the methods taking a marshalling object instead", since = "2.6.1")
  def importProject(project: Identifier, inputStream: InputStream, resources: ResourceLoader = EmptyResourceManager): Unit = {
    throw new UnsupportedOperationException("The configured workspace provider does not support importing projects!")
  }

  /**
    * Generic export method that marshals the project as implemented in the given [[ProjectMarshallingTrait]] object.
    *
    * @param projectName
    * @param outputStream
    * @param projectMarshalling object that defines how the project should be marshaled.
    * @return
    */
  def exportProject(projectName: Identifier,
                    outputStream: OutputStream,
                    projectMarshalling: ProjectMarshallingTrait): String = {
    val projectOpt = readProjects().find(_.id == projectName)
    projectOpt match {
      case Some(project) =>
        projectMarshalling.marshal(project, outputStream, this)
      case _ =>
        throw new IllegalArgumentException("Project " + projectName + " does not exists!")
    }
  }

  /**
    * Generic project import method that unmarshals the project as implemented in the given [[ProjectMarshallingTrait]] object.
    *
    * @param projectName
    * @param inputStream
    * @param projectMarshalling object that defines how the project should be unmarshaled.
    */
  def importProjectMarshaled(projectName: Identifier,
                             inputStream: InputStream,
                             projectMarshalling: ProjectMarshallingTrait): Unit = {
    val projectOpt = readProjects().find(_.id == projectName)
    projectOpt match {
      case Some(project) =>
        throw new IllegalArgumentException("Project " + projectName + " does already exist!")
      case _ =>
        projectMarshalling.unmarshalAndImport(projectName, this, inputStream)
    }
  }
}