package org.silkframework.workspace.xml

import java.io.{InputStream, OutputStream}
import java.util.zip.{ZipEntry, ZipInputStream, ZipOutputStream}

import org.silkframework.runtime.plugin.Plugin
import org.silkframework.runtime.resource.{InMemoryResourceManager, ResourceLoader, ResourceManager, UrlResourceManager}
import org.silkframework.util.Identifier
import org.silkframework.workspace.io.WorkspaceIO
import org.silkframework.workspace.resources.ResourceRepository
import org.silkframework.workspace.{ProjectConfig, ProjectMarshallingTrait, WorkspaceProvider}

/**
  * Created on 6/27/16.
  */
@Plugin(
  id = "xmlZipMarshalling",
  label = "XML zip project",
  description = "XML zip marshalling of Silk projects for importing and exporting."
)
case class XmlZipProjectMarshaling() extends ProjectMarshallingTrait {
  val id = "xmlZip"

  val name = "XML zip file"

  /**
    * Marshals the project.
    *
    * @param project
    * @param outputStream      The output stream the marshaled project data should be written to.
    * @param workspaceProvider The workspace provider the project is coming from.
    * @return
    */
  override def marshalProject(project: ProjectConfig,
                       outputStream: OutputStream,
                       workspaceProvider: WorkspaceProvider,
                       resourceManager: ResourceManager): String = {
    // Open ZIP
    val zip = new ZipOutputStream(outputStream)
    val xmlResourceManager = InMemoryResourceManager()
    val xmlWorkspaceProvider = new XmlWorkspaceProvider(xmlResourceManager)

    // Load project into temporary XML workspace provider
    exportProject(project.id, workspaceProvider, exportToWorkspace = xmlWorkspaceProvider,
                  Some(resourceManager), Some(getProjectResources(xmlWorkspaceProvider, project.id)))

    // Go through all files and create a ZIP entry for each
    putResources(zip, xmlResourceManager.child(project.id), "")

    // Close ZIP
    zip.close()

    //Return proposed file name
    project.id.toString + ".zip"
  }

  /**
    * Unmarshals the project
    *
    * @param projectName
    * @param workspaceProvider The workspace provider the project should be imported into.
    * @param inputStream       The marshaled project data from an [[InputStream]].
    */
  override def unmarshalProject(projectName: Identifier,
                                workspaceProvider: WorkspaceProvider,
                                resourceManager: ResourceManager,
                                inputStream: InputStream): Unit = {
    val xmlWorkspaceProvider = createWorkspaceFromInputStream(Some(projectName), inputStream)
    val projectResources = getProjectResources(xmlWorkspaceProvider, projectName)
    importProject(projectName, workspaceProvider, importFromWorkspace = xmlWorkspaceProvider, Some(projectResources), importResources = Some(resourceManager))
  }

  override def marshalWorkspace(outputStream: OutputStream,
                                workspaceProvider: WorkspaceProvider,
                                resourceRepository: ResourceRepository): String = {
    // Open ZIP
    val zip = new ZipOutputStream(outputStream)
    val xmlResourceManager = InMemoryResourceManager()
    val xmlWorkspaceProvider = new XmlWorkspaceProvider(xmlResourceManager)

    // Load all projects into temporary XML workspace provider
    for(project <- workspaceProvider.readProjects()) {
      exportProject(project.id, workspaceProvider, exportToWorkspace = xmlWorkspaceProvider,
        Some(resourceRepository.get(project.id)), Some(getProjectResources(xmlWorkspaceProvider, project.id)))
    }

    // Go through all files and create a ZIP entry for each
    putResources(zip, xmlResourceManager, "")

    // Close ZIP
    zip.close()

    //Return proposed file name
    "workspace.zip"
  }

  override def unmarshalWorkspace(workspaceProvider: WorkspaceProvider,
                                  resourceRepository: ResourceRepository,
                                  inputStream: InputStream): Unit = {
    val xmlWorkspaceProvider = createWorkspaceFromInputStream(None, inputStream)
    val projects = xmlWorkspaceProvider.readProjects()

    for(project <- projects) {
      val projectResources = getProjectResources(xmlWorkspaceProvider, project.id)
      importProject(project.id, workspaceProvider, importFromWorkspace = xmlWorkspaceProvider, Some(projectResources), importResources = Some(resourceRepository.get(project.id)))
    }
  }

  private def createWorkspaceFromInputStream(projectName: Option[Identifier],
                                             inputStream: InputStream): XmlWorkspaceProvider = {
    val resourceManager = UrlResourceManager(InMemoryResourceManager())
    // Open ZIP
    val zip = new ZipInputStream(inputStream)

    // Read all ZIP entries
    try {
      val projectRes = if(projectName.isDefined) resourceManager.child(projectName.get) else resourceManager
      var entry = zip.getNextEntry
      while (entry != null) {
        if (!entry.isDirectory) {
          projectRes.getInPath(entry.getName).writeStream(zip)
        }
        zip.closeEntry()
        entry = zip.getNextEntry
      }
    } finally {
      // Close ZIP and reload
      zip.close()
    }
    new XmlWorkspaceProvider(resourceManager)
  }

  private def getProjectResources(provider: XmlWorkspaceProvider, project: Identifier): ResourceManager = {
    provider.resources.child(project).child("resources")
  }

  private def putResources(zip: ZipOutputStream, loader: ResourceLoader, basePath: String): Unit = {
    for (resName <- loader.list) {
      zip.putNextEntry(new ZipEntry(basePath + resName))
      zip.write(loader.get(resName).loadAsBytes)
    }
    for (childName <- loader.listChildren) {
      putResources(zip, loader.child(childName), basePath + childName + "/")
    }
  }

  /** Handler for file suffix */
  override def suffix: Option[String] = Some("zip")
}
