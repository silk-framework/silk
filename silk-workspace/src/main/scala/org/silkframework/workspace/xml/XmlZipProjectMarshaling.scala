package org.silkframework.workspace.xml

import java.io.{File, OutputStream}
import java.util.zip.ZipFile

import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource._
import org.silkframework.runtime.resource.zip.{ZipFileResourceLoader, ZipOutputStreamResourceManager}
import org.silkframework.runtime.validation.NotFoundException
import org.silkframework.util.Identifier
import org.silkframework.workspace.resources.ResourceRepository
import org.silkframework.workspace.{Project, ProjectMarshallingTrait, WorkspaceProvider}

abstract class XmlZipProjectMarshaling extends ProjectMarshallingTrait {

  def includeResources: Boolean

  /**
    * Marshals the project from the in-memory [[Project]] object and the given resource manager.
    *
    * @param project A project object that contains all tasks in-memory.
    * @param outputStream The output stream for the marshalling output.
    * @param resourceManager Marshal resources from this resource manager.
    * @return The proposed ZIP file name.
    */
  override def marshalProject(project: Project,
                              outputStream: OutputStream,
                              resourceManager: ResourceManager)
                             (implicit userContext: UserContext): String = {
    val zipResourceManager = new ZipOutputStreamResourceManager(outputStream)
    try {
      val outputWorkspaceProvider = new XmlWorkspaceProvider(zipResourceManager)
      val exportResources = getProjectResources(outputWorkspaceProvider, project.config.id)

      exportProject(project, outputWorkspaceProvider, resourceManager, exportResources, includeResources)
    } finally {
      zipResourceManager.close()
    }

    //Return proposed file name
    project.config.id.toString + ".zip"
  }

  /**
    * Unmarshals the project
    *
    * @param projectName - name of the project
    * @param workspaceProvider The workspace provider the project should be imported into.
    * @param file       The marshaled project file.
    */
  override def unmarshalProject(projectName: Identifier,
                                workspaceProvider: WorkspaceProvider,
                                resourceManager: ResourceManager,
                                file: File)
                                (implicit userContext: UserContext): Unit = {
    val zip = new ZipFile(file)
    try {
      var resourceLoader: ResourceLoader = ZipFileResourceLoader(zip)
      if(!resourceLoader.list.contains("config.xml")) {
        if (resourceLoader.listChildren.nonEmpty) {
          resourceLoader = resourceLoader.child(resourceLoader.listChildren.head)
        } else {
          throw new NotFoundException("No project found in given zip file. Imported nothing.")
        }
      }
      resourceLoader = new CombinedResourceLoader(children = Map(projectName.toString -> resourceLoader))
      val importResources = ReadOnlyResourceManager(resourceLoader)

      val xmlWorkspaceProvider = new XmlWorkspaceProvider(importResources)
      val projectResources = getProjectResources(xmlWorkspaceProvider, projectName)
      importProject(projectName, workspaceProvider, importFromWorkspace = xmlWorkspaceProvider, resourceManager, importResources = projectResources, includeResources)
    } finally {
      zip.close()
    }
  }

  override def marshalWorkspace(outputStream: OutputStream,
                                projects: Seq[Project],
                                resourceRepository: ResourceRepository)
                               (implicit userContext: UserContext): String = {
    val zipResourceManager = new ZipOutputStreamResourceManager(outputStream)
    try {
      val xmlWorkspaceProvider = new XmlWorkspaceProvider(zipResourceManager)

      // Load all projects into temporary XML workspace provider
      for (project <- projects) {
        val projectResources = resourceRepository.get(project.config.id)
        exportProject(project, xmlWorkspaceProvider, projectResources, getProjectResources(xmlWorkspaceProvider, project.config.id), alsoExportResources = includeResources)
      }
    } finally {
      // Close ZIP
      zipResourceManager.close()
    }

    //Return proposed file name
    "workspace.zip"
  }

  override def unmarshalWorkspace(workspaceProvider: WorkspaceProvider,
                                  resourceRepository: ResourceRepository,
                                  file: File)
                                 (implicit userContext: UserContext): Unit = {
    val zip = new ZipFile(file)
    try {
      val resourceManager: ResourceManager = ReadOnlyResourceManager(ZipFileResourceLoader(zip))
      val xmlWorkspaceProvider = new XmlWorkspaceProvider(resourceManager)
      val projects = xmlWorkspaceProvider.readProjects()

      for (project <- projects) {
        val projectResources = getProjectResources(xmlWorkspaceProvider, project.id)
        importProject(project.id, workspaceProvider, importFromWorkspace = xmlWorkspaceProvider,
          resourceRepository.get(project.id), importResources = projectResources, includeResources)
      }
    } finally {
      zip.close()
    }
  }

  private def getProjectResources(provider: XmlWorkspaceProvider, project: Identifier): ResourceManager = {
    provider.resources.child(project).child("resources")
  }

  /** Handler for file suffix */
  override def fileExtension: String = "zip"

  override def mediaType: Option[String] = Some("application/zip")
}

object XmlZipProjectMarshaling {

  def apply(includeResources: Boolean = true): XmlZipProjectMarshaling = {
    if(includeResources) {
      XmlZipWithResourcesProjectMarshaling()
    } else {
      XmlZipWithoutResourcesProjectMarshaling()
    }
  }

}