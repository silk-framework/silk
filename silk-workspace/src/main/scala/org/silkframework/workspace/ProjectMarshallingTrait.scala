package org.silkframework.workspace

import org.silkframework.config.CustomTask
import org.silkframework.dataset.{Dataset, DatasetSpec}
import org.silkframework.rule.{LinkSpec, TransformSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.AnyPlugin
import org.silkframework.runtime.plugin.annotations.PluginType
import org.silkframework.runtime.resource.ResourceManager
import org.silkframework.util.Identifier
import org.silkframework.workspace.activity.workflow.Workflow
import org.silkframework.workspace.io.WorkspaceIO
import org.silkframework.workspace.io.WorkspaceIO.copyResources
import org.silkframework.workspace.resources.ResourceRepository

import java.io.{File, OutputStream}

/**
  * Trait defining methods for marshalling and unmarshalling of Silk projects.
  */
@PluginType()
trait ProjectMarshallingTrait extends AnyPlugin {
  /**
    * A unique ID, so this marshaller can be distinguished from other marshallers
    */
  def id: String

  /** A descriptive name of this marshaller */
  def name: String

  /** The preferred file extension, e.g., "zip". */
  def fileExtension: String

  /** Optional qualifier to distinguish file names with the same extension, e.g., "no-resoures". */
  def qualifier: Option[String] = None

  /** MIME-Type */
  def mediaType: Option[String]

  /** If true, this plugin is the preferred marshaller for the given suffix and mediaType */
  def isPreferred: Boolean = true

  /**
    * Marshals the project from the in-memory [[Project]] object and the given resource manager.
    *
    * @param project         The in-memory [[Project]] object from the workspace.
    * @param outputStream    The output stream the marshaled project data should be written to.
    * @param resourceManager The resource manager from which project resources should be marshaled.
    * @return The proposed file name for the marshaled resource.
    */
  def marshalProject(project: Project,
                     outputStream: OutputStream,
                     resourceManager: ResourceManager)
                    (implicit userContext: UserContext): String

  /**
    * Unmarshals the project
    *
    * @param projectName
    * @param workspaceProvider The workspace provider the project should be imported into.
    * @param file              The marshaled project file.
    */
  def unmarshalProject(projectName: Identifier,
                       workspaceProvider: WorkspaceProvider,
                       resourceManager: ResourceManager,
                       file: File)
                      (implicit userContext: UserContext): Unit

  /**
    * Marshals the entire workspace from the in-memory [[Project]] objects.
    *
    * @param outputStream       The output stream the marshaled project data should be written to.
    * @param projects           All projects from the workspace that should be marshaled.
    * @param resourceRepository The resource repository from which project resources should be marshaled.
    * @return The proposed file name for the marshaled resource.
    */
  def marshalWorkspace(outputStream: OutputStream,
                       projects: Seq[Project],
                       resourceRepository: ResourceRepository)
                      (implicit userContext: UserContext): String


  /**
    * Unmarshals and imports the entire workspace.
    *
    * @param workspaceProvider The workspace provider the projects should be imported into.
    * @param file       The marshaled project file.
    * @return
    */
  def unmarshalWorkspace(workspaceProvider: WorkspaceProvider,
                         resourceRepository: ResourceRepository,
                         file: File)
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
                              resources: ResourceManager,
                              importResources: ResourceManager,
                              alsoCopyResources: Boolean)
                             (implicit userContext: UserContext): Unit = {
    // Create new empty project
    for ((project, index) <- importFromWorkspace.readProjects().filter(_.id == projectName).zipWithIndex) {
      val targetProject = if (index == 0) projectName else projectName + index
      // Reset URI
      val projectConfig = project.copy(id = targetProject, projectResourceUriOpt = None)

      workspaceProvider.importProject(projectConfig, importFromWorkspace, importResources, resources, alsoCopyResources)
    }
  }

  protected def exportProject(projectName: Identifier,
                              workspaceProvider: WorkspaceProvider,
                              exportToWorkspace: WorkspaceProvider,
                              resources: ResourceManager,
                              exportToResources: ResourceManager,
                              alsoExportResources: Boolean)
                             (implicit userContext: UserContext): Unit = {
    // Export project
    val project = workspaceProvider.readProjects().find(_.id == projectName).get
    WorkspaceIO.copyProject(workspaceProvider, exportToWorkspace, resources, exportToResources, project, alsoExportResources)
  }

  protected def exportProject(project: Project,
                              outputWorkspaceProvider: WorkspaceProvider,
                              resources: ResourceManager,
                              exportToResources: ResourceManager,
                              alsoExportResources: Boolean)
                             (implicit userContext: UserContext): Unit = {
    // Load project into temporary XML workspace provider
    val updatedProjectConfig = project.config.copy(projectResourceUriOpt = Some(project.config.resourceUriOrElseDefaultUri))
    val projectId = updatedProjectConfig.id
    outputWorkspaceProvider.putProject(updatedProjectConfig)
    outputWorkspaceProvider.putTags(updatedProjectConfig.id, project.tagManager.allTags())
    outputWorkspaceProvider.projectVariables(updatedProjectConfig.id).putVariables(project.templateVariables.all)
    if(alsoExportResources) {
      copyResources(resources, exportToResources)
    }
    for(dataset <- project.tasks[DatasetSpec[Dataset]]) {
      outputWorkspaceProvider.putTask(projectId, dataset, resources)
    }
    for(transformTask <- project.tasks[TransformSpec]) {
      outputWorkspaceProvider.putTask(projectId, transformTask, resources)
    }
    for(task <- project.tasks[LinkSpec]) {
      outputWorkspaceProvider.putTask(projectId, task, resources)
    }
    for(task <- project.tasks[Workflow]) {
      outputWorkspaceProvider.putTask(projectId, task, resources)
    }
    for(task <- project.tasks[CustomTask]) {
      outputWorkspaceProvider.putTask(projectId, task, resources)
    }
  }
}
