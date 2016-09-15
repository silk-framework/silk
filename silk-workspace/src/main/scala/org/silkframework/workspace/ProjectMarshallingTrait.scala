package org.silkframework.workspace

import java.io.{InputStream, OutputStream}

import org.silkframework.config.LinkSpec
import org.silkframework.dataset.DatasetTask
import org.silkframework.util.Identifier
import org.silkframework.workspace.activity.workflow.Workflow
import org.silkframework.workspace.io.WorkspaceIO

import scala.reflect.ClassTag

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
  def marshal(project: ProjectConfig,
              outputStream: OutputStream,
              workspaceProvider: WorkspaceProvider): String

  /**
    * Unmarshals the project
    *
    * @param projectName
    * @param workspaceProvider The workspace provider the project should be imported into.
    * @param inputStream       The marshaled project data from an [[InputStream]].
    */
  def unmarshalAndImport(projectName: Identifier,
                         workspaceProvider: WorkspaceProvider,
                         inputStream: InputStream): Unit

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
                              importFromWorkspace: WorkspaceProvider): Unit = {
    // Create new empty project
    for ((project, index) <- importFromWorkspace.readProjects().zipWithIndex) {
      val targetProject = if (index == 0) projectName else projectName + index
      // Reset URI
      val projectConfig = project.copy(id = targetProject, projectResourceUriOpt = None)

      WorkspaceIO.copyProject(importFromWorkspace, workspaceProvider, projectConfig)
    }
  }

  protected def exportProject(projectName: Identifier,
                              workspaceProvider: WorkspaceProvider,
                              exportToWorkspace: WorkspaceProvider): Unit = {
    // Export project
    val project = workspaceProvider.readProjects().find(_.id == projectName).get
    WorkspaceIO.copyProject(workspaceProvider, exportToWorkspace, project)
  }
}
