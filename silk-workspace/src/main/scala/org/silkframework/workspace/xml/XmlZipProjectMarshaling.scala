package org.silkframework.workspace.xml

import java.io.{File, InputStream, OutputStream}
import java.util.zip.{ZipFile, ZipInputStream, ZipOutputStream}

import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.Plugin
import org.silkframework.runtime.resource._
import org.silkframework.runtime.validation.{NotFoundException, ValidationException}
import org.silkframework.util.Identifier
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

  def id: String = XmlZipProjectMarshaling.marshallerId

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
                              resourceManager: ResourceManager)
                             (implicit userContext: UserContext): String = {
    // Open ZIP
    val zip = new ZipOutputStream(outputStream)
    try {
      val zipResourceManager = ZipOutputStreamResourceManager(zip)
      val xmlWorkspaceProvider = new XmlWorkspaceProvider(zipResourceManager)

      // Load project into temporary XML workspace provider
      exportProject(project.id, workspaceProvider, exportToWorkspace = xmlWorkspaceProvider,
        Some(resourceManager), Some(getProjectResources(xmlWorkspaceProvider, project.id)))
    } finally {
      // Close ZIP
      zip.close()
    }

    //Return proposed file name
    project.id.toString + ".zip"
  }

  /**
    * Unmarshals the project
    *
    * @param projectName
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
      var resourceLoader: ResourceLoader = ZipResourceLoader(zip)
      if(!resourceLoader.list.contains("config.xml")) {
        if (resourceLoader.listChildren.nonEmpty) {
          resourceLoader = resourceLoader.child(resourceLoader.listChildren.head)
        } else {
          throw new NotFoundException("No project found in given zip file. Imported nothing.")
        }
      }
      resourceLoader = new NestedResourceLoader(children = Map(projectName.toString -> resourceLoader))
      val importResources = ReadOnlyResourceManager(resourceLoader)

      val xmlWorkspaceProvider = new XmlWorkspaceProvider(importResources)
      val projectResources = getProjectResources(xmlWorkspaceProvider, projectName)
      importProject(projectName, workspaceProvider, importFromWorkspace = xmlWorkspaceProvider, Some(projectResources), importResources = Some(resourceManager))
    } finally {
      zip.close()
    }
  }

  override def marshalWorkspace(outputStream: OutputStream,
                                workspaceProvider: WorkspaceProvider,
                                resourceRepository: ResourceRepository)
                               (implicit userContext: UserContext): String = {
    // Open ZIP
    val zip = new ZipOutputStream(outputStream)
    try {
      val zipResourceManager = ZipOutputStreamResourceManager(zip)
      val xmlWorkspaceProvider = new XmlWorkspaceProvider(zipResourceManager)

      // Load all projects into temporary XML workspace provider
      for (project <- workspaceProvider.readProjects()) {
        exportProject(project.id, workspaceProvider, exportToWorkspace = xmlWorkspaceProvider,
          Some(resourceRepository.get(project.id)), Some(getProjectResources(xmlWorkspaceProvider, project.id)))
      }
    } finally {
      // Close ZIP
      zip.close()
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
      val resourceManager: ResourceManager = ReadOnlyResourceManager(ZipResourceLoader(zip))
      val xmlWorkspaceProvider = new XmlWorkspaceProvider(resourceManager)
      val projects = xmlWorkspaceProvider.readProjects()

      for (project <- projects) {
        val projectResources = getProjectResources(xmlWorkspaceProvider, project.id)
        importProject(project.id, workspaceProvider, importFromWorkspace = xmlWorkspaceProvider, Some(projectResources), importResources = Some(resourceRepository.get(project.id)))
      }
    } finally {
      zip.close()
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
      var stripPrefix = ""
      var configFound = false
      var stop = false
      while (entry != null && !stop) {
        if (!entry.isDirectory) {
          val entryName = entry.getName
          val nameParts = entry.getName.split("/")
          if(stripPrefix == "" && projectName.isDefined && (nameParts.last == "config.xml")) {
            /* If this is a workspace zip, but a single project should be imported, pick the first project from the workspace
               This is the fastest way to address this issue, since only one project is actually copied from the ZIP stream.
               FIXME: The fact that config.xml is the first file to appear in any project folder may become invalid in the future
             */
            configFound = true
            stripPrefix = if(nameParts.size == 2) nameParts(0) + "/" else ""
          }
          if(entry.getName.startsWith(stripPrefix)) {
            /* FIXME: If this is a workspace zip only the first project is imported, all others are ignored
                      A better solution would probably be to let the user choose which project to import from the workspace zip. */
            projectRes.getInPath(entry.getName.stripPrefix(stripPrefix)).writeStream(zip)
          } else {
            stop = true // Since project files are ordered in the stream, we can stop if another project pops up
          }
        }
        zip.closeEntry()
        entry = zip.getNextEntry
      }
      if(projectName.isDefined && !configFound) {
        // No project found, but project expected
        throw new ValidationException("No project found in given zip file. Imported nothing!")
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

  /** Handler for file suffix */
  override def suffix: Option[String] = Some("zip")
}

object XmlZipProjectMarshaling {

  val marshallerId = "xmlZip"

}
