package org.silkframework.workspace.xml

import java.io.{File, OutputStream}
import java.util.logging.{Level, Logger}
import java.util.zip.ZipFile

import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource._
import org.silkframework.runtime.resource.zip.{ZipFileResourceLoader, ZipOutputStreamResourceManager}
import org.silkframework.runtime.validation.NotFoundException
import org.silkframework.util.Identifier
import org.silkframework.workspace.changes.ChangeJournalStore
import org.silkframework.workspace.resources.ResourceRepository
import org.silkframework.workspace.xml.XmlZipProjectMarshaling.{JOURNAL_FOLDER, log}
import org.silkframework.workspace.{Project, ProjectMarshallingTrait, WorkspaceProvider}

import scala.util.control.NonFatal

/**
  * Project and workspace archives as ZIP files of the XML workspace layout. The change journal of a project travels
  * in a `changes` folder next to its task folders, with its user data: see [[exportJournal]] and [[importJournal]].
  */
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
                              resourceManager: ResourceManager,
                              exportGroups: Boolean = false,
                              exportUserData: Boolean = true)
                             (implicit userContext: UserContext): String = {
    val zipResourceManager = new ZipOutputStreamResourceManager(outputStream)
    try {
      val outputWorkspaceProvider = new XmlWorkspaceProvider(zipResourceManager)
      val exportResources = getProjectResources(outputWorkspaceProvider, project.config.id)

      exportProject(project, outputWorkspaceProvider, resourceManager, exportResources, includeResources, exportGroups = exportGroups, exportUserData = exportUserData)
      exportJournal(project, zipResourceManager, exportUserData)
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
      var projectLoader: ResourceLoader = ZipFileResourceLoader(zip)
      if(!projectLoader.list.contains("config.xml")) {
        if (projectLoader.listChildren.nonEmpty) {
          projectLoader = projectLoader.child(projectLoader.listChildren.head)
        } else {
          throw new NotFoundException("No project found in given zip file. Imported nothing.")
        }
      }
      val resourceLoader = new CombinedResourceLoader(children = Map(projectName.toString -> projectLoader))
      val importResources = ReadOnlyResourceManager(resourceLoader)

      val xmlWorkspaceProvider = new XmlWorkspaceProvider(importResources)
      val projectResources = getProjectResources(xmlWorkspaceProvider, projectName)
      importProject(projectName, workspaceProvider, importFromWorkspace = xmlWorkspaceProvider, resourceManager, importResources = projectResources, includeResources)
      importJournal(projectName, projectLoader)
    } finally {
      zip.close()
    }
  }

  override def marshalWorkspace(outputStream: OutputStream,
                                projects: Seq[Project],
                                resourceRepository: ResourceRepository,
                                exportGroups: Boolean = false,
                                exportUserData: Boolean = true)
                               (implicit userContext: UserContext): String = {
    val zipResourceManager = new ZipOutputStreamResourceManager(outputStream)
    try {
      val xmlWorkspaceProvider = new XmlWorkspaceProvider(zipResourceManager)

      // Load all projects into temporary XML workspace provider
      for (project <- projects) {
        val projectResources = resourceRepository.get(project.config.id)
        exportProject(project, xmlWorkspaceProvider, projectResources, getProjectResources(xmlWorkspaceProvider, project.config.id), exportResources = includeResources, exportGroups = exportGroups, exportUserData = exportUserData)
        exportJournal(project, zipResourceManager, exportUserData)
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
      val zipLoader = ZipFileResourceLoader(zip)
      val resourceManager: ResourceManager = ReadOnlyResourceManager(zipLoader)
      val xmlWorkspaceProvider = new XmlWorkspaceProvider(resourceManager)
      val projects = xmlWorkspaceProvider.readProjects()

      for (project <- projects) {
        val projectResources = getProjectResources(xmlWorkspaceProvider, project.id)
        importProject(project.id, workspaceProvider, importFromWorkspace = xmlWorkspaceProvider,
          resourceRepository.get(project.id), importResources = projectResources, includeResources)
        importJournal(project.id, zipLoader.child(project.id))
      }
    } finally {
      zip.close()
    }
  }

  private def getProjectResources(provider: XmlWorkspaceProvider, project: Identifier): ResourceManager = {
    provider.resources.child(project).child("resources")
  }

  /** The journal is user data, who changed what and when, so it travels with the user data only. */
  private def exportJournal(project: Project, zipResourceManager: ResourceManager, exportUserData: Boolean): Unit = {
    if(exportUserData) {
      ChangeJournalStore().exportJournal(project.id, zipResourceManager.child(project.id).child(JOURNAL_FOLDER))
    }
  }

  /** Called for every import, so that an archive without a journal leaves the project with an empty one rather
    * than a stale one. A journal that cannot be imported is logged and does not fail the import of the project. */
  private def importJournal(project: Identifier, projectLoader: ResourceLoader): Unit = {
    try {
      ChangeJournalStore().importJournal(project, projectLoader.child(JOURNAL_FOLDER))
    } catch {
      case NonFatal(ex) =>
        log.log(Level.WARNING, s"The change journal of project '$project' could not be imported from the archive; the project starts without history.", ex)
    }
  }

  /** Handler for file suffix */
  override def fileExtension: String = "zip"

  override def mediaType: Option[String] = Some("application/zip")
}

object XmlZipProjectMarshaling {

  private val log = Logger.getLogger(classOf[XmlZipProjectMarshaling].getName)

  /** The folder of a project's change journal in the archive, next to `resources` and the task folders. */
  final val JOURNAL_FOLDER = "changes"

  def apply(includeResources: Boolean = true): XmlZipProjectMarshaling = {
    if(includeResources) {
      XmlZipWithResourcesProjectMarshaling()
    } else {
      XmlZipWithoutResourcesProjectMarshaling()
    }
  }

}