package de.fuberlin.wiwiss.silk.workspace

import java.io._
import java.util.zip.{ZipInputStream, ZipEntry, ZipOutputStream}
import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin
import de.fuberlin.wiwiss.silk.util.FileUtils._
import de.fuberlin.wiwiss.silk.config.Prefixes
import de.fuberlin.wiwiss.silk.runtime.resource.{ResourceLoader, FileResourceManager, ResourceManager}
import de.fuberlin.wiwiss.silk.util.Identifier
import de.fuberlin.wiwiss.silk.util.XMLUtils._
import de.fuberlin.wiwiss.silk.workspace.modules.ModulePlugin
import de.fuberlin.wiwiss.silk.workspace.modules.dataset.DatasetModulePlugin
import de.fuberlin.wiwiss.silk.workspace.modules.linking.LinkingModulePlugin
import de.fuberlin.wiwiss.silk.workspace.modules.transform.TransformModulePlugin
import de.fuberlin.wiwiss.silk.workspace.modules.workflow.WorkflowModulePlugin
import scala.reflect.ClassTag
import scala.xml.XML

@Plugin(
  id = "file",
  label = "Filesystem",
  description = "Workspace on filesystem"
)
case class FileWorkspaceProvider(dir: String) extends WorkspaceProvider {

  private val res = new FileResourceManager(dir)

  @volatile
  private var plugins = Map[Class[_], ModulePlugin[_]]()

  // Register all module types
  registerModule(new DatasetModulePlugin())
  registerModule(new LinkingModulePlugin())
  registerModule(new TransformModulePlugin())
  registerModule(new WorkflowModulePlugin())

  def registerModule[T: ClassTag](plugin: ModulePlugin[T]) = {
    val clazz = implicitly[ClassTag[T]].runtimeClass
    plugins += (clazz -> plugin)
  }

  override def readProjects(): Seq[ProjectConfig] = {
    for(projectName <- res.listChildren) yield {
      val configXML = XML.load(res.child(projectName).get("config.xml").load)
      val prefixes = Prefixes.fromXML((configXML \ "Prefixes").head)
      ProjectConfig(projectName, prefixes)
    }
  }

  override def putProject(config: ProjectConfig): Unit = {
    val configXMl =
      <ProjectConfig>
        { config.prefixes.toXML }
      </ProjectConfig>
    res.child(config.id).put("config.xml") { os => configXMl.write(os) }
  }

  override def deleteProject(name: Identifier): Unit = {
    res.delete(name)
  }

  /**
   * Retrieves the project resources (e.g. associated files).
   */
  override def projectResources(name: Identifier): ResourceManager = {
    res.child(name).child("resources")
  }

  /**
   * Retrieves the project cache folder.
   */
  def projectCache(name: Identifier): ResourceManager = {
    res.child(name)
  }

  override def readTasks[T: ClassTag](project: Identifier): Seq[(Identifier, T)] = {
    plugin[T].loadTasks(res.child(project).child(plugin[T].prefix), res.child(project).child("resources")).toSeq
  }

  override def putTask[T: ClassTag](project: Identifier, data: T): Unit = {
    plugin[T].writeTask(data, res.child(project).child(plugin[T].prefix))
  }

  override def deleteTask[T: ClassTag](project: Identifier, task: Identifier): Unit = {
    plugin[T].removeTask(task, res.child(project).child(plugin[T].prefix))
  }

  override def exportProject(project: Identifier, outputStream: OutputStream): String = {
    // Open ZIP
    val zip = new ZipOutputStream(outputStream)
    val projectDir = res.child(project).asInstanceOf[FileResourceManager].baseDir // TODO allow exporting non file-based workspaces
    require(projectDir.exists, s"Project $project does not exist.")

    // Recursively lists all files in the given directory
    def listFiles(file: File): List[File] = {
      if(file.isFile) file :: Nil
      else file.listFiles.toList.flatMap(listFiles)
    }

    // Go through all files and create a ZIP entry for each
    for(file <- listFiles(projectDir)) {
      val relativePath = projectDir.toPath.relativize(file.toPath).toString.replace("\\", "/")
      zip.putNextEntry(new ZipEntry(relativePath))
      val in = new BufferedInputStream(new FileInputStream(file))
      var b = in.read()
      while (b > -1) {
        zip.write(b)
        b = in.read()
      }
      in.close()
      zip.closeEntry()
    }

    // Close ZIP
    zip.close()

    //Return proposed file name
    project + ".zip"
  }

  //TODO if an import fails, delete all already created files!
  override def importProject(project: Identifier, inputStream: InputStream, resources: ResourceLoader): Unit = {
    // Open ZIP
    val zip = new ZipInputStream(inputStream)
    val projectDir = res.child(project).asInstanceOf[FileResourceManager].baseDir // TODO allow importing non file-based workspaces
    require(!projectDir.exists, s"Project $project already exists.")

    // Read all ZIP entries
    var entry = zip.getNextEntry
    while(entry != null) {
      if(!entry.isDirectory) {
        val file = projectDir + ("/" + entry.getName)
        file.getParentFile.mkdirs()
        val out = new BufferedOutputStream(new FileOutputStream(file))
        var b = zip.read()
        while (b > -1) {
          out.write(b)
          b = zip.read()
        }
        out.close()
      }
      zip.closeEntry()
      entry = zip.getNextEntry
    }

    // Close ZIP and reload
    zip.close()
  }

  private def plugin[T: ClassTag] = {
    plugins(implicitly[ClassTag[T]].runtimeClass).asInstanceOf[ModulePlugin[T]]
  }
}
