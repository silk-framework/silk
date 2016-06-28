package org.silkframework.workspace

import java.io.{InputStream, OutputStream}

import org.silkframework.config.{LinkSpecification, TransformSpecification}
import org.silkframework.dataset.Dataset
import org.silkframework.util.Identifier
import org.silkframework.workspace.activity.workflow.Workflow

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
    * @param outputStream The output stream the marshaled project data should be written to.
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
    * Import tasks of a specific type.
 *
    * @param sourceProjectName
    * @param targetProjectName
    * @param workspaceProvider the workspace provider where the project should be imported into.
    * @param importFromWorkspace the workspace from which the project should be imported from.
    * @tparam T the task type
    */
  private def importTasks[T: ClassTag](sourceProjectName: Identifier,
                                         targetProjectName: Identifier,
                                         workspaceProvider: WorkspaceProvider,
                                         importFromWorkspace: WorkspaceProvider): Unit = {
    for((id, task) <- importFromWorkspace.readTasks(sourceProjectName)) {
      workspaceProvider.putTask(targetProjectName, id, task)
    }
  }

  /**
    *
    * @param project
    * @param workspaceProvider
    * @param exportWorkspace
    * @tparam T the task type
    */
  private def exportTasks[T: ClassTag](project: Identifier,
                                         workspaceProvider: WorkspaceProvider,
                                         exportWorkspace: WorkspaceProvider): Unit = {
    val tasks = workspaceProvider.readTasks[T](project)
    for((id, task) <- tasks) {
      exportWorkspace.putTask[T](project, id, task)
    }
  }

  /**
    * Imports a project from one workspace provider to the other one.
    * @param projectName
    * @param workspaceProvider
    * @param importFromWorkspace
    */
  protected def importProject(projectName: Identifier,
                              workspaceProvider: WorkspaceProvider,
                              importFromWorkspace: WorkspaceProvider): Unit = {
    // Create new empty project
    for((project, index) <- importFromWorkspace.readProjects().zipWithIndex) {
      val targetProject = if(index == 0) projectName else projectName + index
      workspaceProvider.putProject(project.copy(id = targetProject))

      // Import tasks into workspace provider
      importTasks[Dataset](projectName, targetProject, workspaceProvider, importFromWorkspace = importFromWorkspace)
      importTasks[TransformSpecification](projectName, targetProject, workspaceProvider, importFromWorkspace = importFromWorkspace)
      importTasks[LinkSpecification](projectName, targetProject, workspaceProvider, importFromWorkspace = importFromWorkspace)
      importTasks[Workflow](projectName, targetProject, workspaceProvider, importFromWorkspace = importFromWorkspace)
    }
  }

  protected def exportProject(projectName: Identifier,
                              workspaceProvider: WorkspaceProvider,
                              exportToWorkspace: WorkspaceProvider): Unit = {
    // Export project
    val project = workspaceProvider.readProjects().find(_.id == projectName).get
    exportToWorkspace.putProject(project)

    // Export tasks
    exportTasks[Dataset](projectName, workspaceProvider, exportWorkspace = exportToWorkspace)
    exportTasks[TransformSpecification](projectName, workspaceProvider, exportWorkspace = exportToWorkspace)
    exportTasks[LinkSpecification](projectName, workspaceProvider, exportWorkspace = exportToWorkspace)
    exportTasks[Workflow](projectName, workspaceProvider, exportWorkspace = exportToWorkspace)
  }
}
