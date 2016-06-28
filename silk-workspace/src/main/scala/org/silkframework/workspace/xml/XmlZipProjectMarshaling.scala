package org.silkframework.workspace.xml

import java.io.{OutputStream, InputStream}
import java.util.zip.{ZipEntry, ZipOutputStream, ZipInputStream}

import org.silkframework.runtime.plugin.Plugin
import org.silkframework.runtime.resource.{ResourceLoader, InMemoryResourceManager}
import org.silkframework.util.Identifier
import org.silkframework.workspace.{ProjectConfig, WorkspaceProvider, ProjectMarshallingTrait}

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
  override def marshal(project: ProjectConfig,
                       outputStream: OutputStream,
                       workspaceProvider: WorkspaceProvider): String = {
    // Open ZIP
    val zip = new ZipOutputStream(outputStream)
    val resourceManager = InMemoryResourceManager()
    val xmlWorkspaceProvider = new XmlWorkspaceProvider(resourceManager)
    // Load project into temporary XML workspace provider
    exportProject(project.id, workspaceProvider, exportToWorkspace = xmlWorkspaceProvider)

    // Go through all files and create a ZIP entry for each
    putResources(resourceManager.child(project.id), "")

    def putResources(loader: ResourceLoader, basePath: String): Unit = {
      for (resName <- loader.list) {
        zip.putNextEntry(new ZipEntry(basePath + resName))
        zip.write(loader.get(resName).loadAsBytes)
      }
      for (childName <- loader.listChildren) {
        putResources(loader.child(childName), basePath + childName + "/")
      }
    }

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
  override def unmarshalAndImport(projectName: Identifier,
                                  workspaceProvider: WorkspaceProvider,
                                  inputStream: InputStream): Unit = {
    val xmlWorkspaceProvider = createWorkspaceFromInputStream(projectName, inputStream)
    importProject(projectName, workspaceProvider, importFromWorkspace = xmlWorkspaceProvider)
  }

  private def createWorkspaceFromInputStream(projectName: Identifier,
                                             inputStream: InputStream): WorkspaceProvider = {
    val resourceManager = InMemoryResourceManager()
    // Open ZIP
    val zip = new ZipInputStream(inputStream)

    // Read all ZIP entries
    try {
      val projectRes = resourceManager.child(projectName)
      var entry = zip.getNextEntry
      while (entry != null) {
        if (!entry.isDirectory) {
          projectRes.getInPath(entry.getName).write(zip)
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

  /** Handler for file suffix */
  override def suffix: Option[String] = Some("zip")
}
