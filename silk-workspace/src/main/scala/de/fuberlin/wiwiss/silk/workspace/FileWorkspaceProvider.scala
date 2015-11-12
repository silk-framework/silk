package de.fuberlin.wiwiss.silk.workspace

import java.io._
import java.util.zip.{ZipInputStream, ZipEntry, ZipOutputStream}
import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin
import de.fuberlin.wiwiss.silk.util.FileUtils._
import de.fuberlin.wiwiss.silk.config.Prefixes
import de.fuberlin.wiwiss.silk.runtime.resource.{Resource, ResourceLoader, FileResourceManager, ResourceManager}
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
    res.child(config.id).get("config.xml").write { os => configXMl.write(os) }
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

  override def putTask[T: ClassTag](project: Identifier, task: Identifier, data: T): Unit = {
    plugin[T].writeTask(data, res.child(project).child(plugin[T].prefix))
  }

  override def deleteTask[T: ClassTag](project: Identifier, task: Identifier): Unit = {
    plugin[T].removeTask(task, res.child(project).child(plugin[T].prefix))
  }

  override def exportProject(project: Identifier, outputStream: OutputStream): String = {
    require(res.listChildren.contains(project.toString), s"Project $project does not exist.")

    // Open ZIP
    val zip = new ZipOutputStream(outputStream)

    // Go through all files and create a ZIP entry for each
    putResources(res.child(project), "")

    def putResources(loader: ResourceLoader, basePath: String): Unit = {
      for(resName <- loader.list) {
        zip.putNextEntry(new ZipEntry(basePath + resName))
        zip.write(loader.get(resName).loadAsBytes)
      }
      for(childName <- loader.listChildren) {
        putResources(loader.child(childName), basePath + childName + "/")
      }
    }

    // Close ZIP
    zip.close()

    //Return proposed file name
    project + ".zip"
  }

  override def importProject(project: Identifier, inputStream: InputStream, resources: ResourceLoader): Unit = {
    require(!res.listChildren.contains(project.toString), s"Project $project already exists.")

    // Open ZIP
    val zip = new ZipInputStream(inputStream)

    // Read all ZIP entries
    try {
      val projectRes = res.child(project)
      var entry = zip.getNextEntry
      while (entry != null) {
        if (!entry.isDirectory) {
          projectRes.getInPath(entry.getName).write(zip)
        }
        zip.closeEntry()
        entry = zip.getNextEntry
      }
    } catch {
      case ex: Throwable =>
        // Something failed. Delete already written project resources and escalate exception.
        res.delete(project)
        throw ex;
    }

    // Close ZIP and reload
    zip.close()
  }

  private def plugin[T: ClassTag] = {
    plugins(implicitly[ClassTag[T]].runtimeClass).asInstanceOf[ModulePlugin[T]]
  }
}
