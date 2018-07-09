package org.silkframework.workspace

import java.io.{InputStream, OutputStream}

import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.ResourceManager
import org.silkframework.util.Identifier
import org.silkframework.workspace.io.WorkspaceIO
import org.silkframework.workspace.resources.ResourceRepository

/**
  * Created on 6/24/16.
  *
  * Trait defining methods for marshalling and unmarshalling of Silk projects.
  */
trait ProjectMarshallingTrait {
  /**
    * A unique ID, so this marshaller can be distinguished from other marshallers
    */
  def id: String

  /** A descriptive name of this marshaller */
  def name: String

  /** Handler for file suffix */
  def suffix: Option[String]

  /**
    * Marshals the project.
    *
    * @param project
    * @param outputStream      The output stream the marshaled project data should be written to.
    * @param workspaceProvider The workspace provider the project is coming from.
    * @return
    */
  def marshalProject(project: ProjectConfig,
                     outputStream: OutputStream,
                     workspaceProvider: WorkspaceProvider,
                     resourceManager: ResourceManager)
                    (implicit userContext: UserContext): String

  /**
    * Unmarshals the project
    *
    * @param projectName
    * @param workspaceProvider The workspace provider the project should be imported into.
    * @param inputStream       The marshaled project data from an [[InputStream]].
    */
  def unmarshalProject(projectName: Identifier,
                       workspaceProvider: WorkspaceProvider,
                       resourceManager: ResourceManager,
                       inputStream: InputStream)
                      (implicit userContext: UserContext): Unit

  /**
    * Marshals the entire workspace.
    *
    * @param outputStream      The output stream the marshaled project data should be written to.
    * @param workspaceProvider The workspace provider the projects are coming from.
    * @return
    */
  def marshalWorkspace(outputStream: OutputStream,
                       workspaceProvider: WorkspaceProvider,
                       resourceRepository: ResourceRepository)
                      (implicit userContext: UserContext): String


  /**
    * Unmarshals and imports the entire workspace.
    *
    * @param workspaceProvider The workspace provider the projects should be imported into.
    * @param inputStream       The marshaled project data from an [[InputStream]].
    * @return
    */
  def unmarshalWorkspace(workspaceProvider: WorkspaceProvider,
                         resourceRepository: ResourceRepository,
                         inputStream: InputStream)
                        (implicit userContext: UserContext): Unit

  /**
    * Helper methods
    */


  /**
    * Imports a project from one workspace provider to the other one.
    *
    * @param projectName
    * @param workspaceProvider
    * @param importFromWorkspace
    */
  protected def importProject(projectName: Identifier,
                              workspaceProvider: WorkspaceProvider,
                              importFromWorkspace: WorkspaceProvider,
                              resources: Option[ResourceManager],
                              importResources: Option[ResourceManager])
                             (implicit userContext: UserContext): Unit = {
    // Create new empty project
    for ((project, index) <- importFromWorkspace.readProjects().filter(_.id == projectName).zipWithIndex) {
      val targetProject = if (index == 0) projectName else projectName + index
      // Reset URI
      val projectConfig = project.copy(id = targetProject, projectResourceUriOpt = None)

      WorkspaceIO.copyProject(importFromWorkspace, workspaceProvider, resources, importResources, projectConfig)
    }
  }

  protected def exportProject(projectName: Identifier,
                              workspaceProvider: WorkspaceProvider,
                              exportToWorkspace: WorkspaceProvider,
                              resources: Option[ResourceManager],
                              exportToResources: Option[ResourceManager])
                             (implicit userContext: UserContext): Unit = {
    // Export project
    val project = workspaceProvider.readProjects().find(_.id == projectName).get
    WorkspaceIO.copyProject(workspaceProvider, exportToWorkspace, resources, exportToResources, project)
  }
}
