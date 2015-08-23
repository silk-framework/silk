package de.fuberlin.wiwiss.silk.workspace

import java.io.File
import de.fuberlin.wiwiss.silk.config.Prefixes
import de.fuberlin.wiwiss.silk.runtime.resource.FileResourceManager
import de.fuberlin.wiwiss.silk.runtime.serialization.XmlFormat
import de.fuberlin.wiwiss.silk.util.Identifier
import de.fuberlin.wiwiss.silk.workspace.modules.ModulePlugin
import scala.reflect.ClassTag
import scala.xml.XML
import de.fuberlin.wiwiss.silk.util.XMLUtils._

class FileWorkspaceProvider(basePath: String) extends WorkspaceProvider {

  private val file = new File(basePath)

  private val res = new FileResourceManager(file)

  @volatile
  private var formatters = Map[Class[_], XmlFormat[_]]()

  @volatile
  private var plugins = Map[Class[_], ModulePlugin[_]]()

  def registerModule[T: ClassTag](plugin: ModulePlugin[_])(implicit format: XmlFormat[T]) = {
    val clazz = implicitly[ClassTag[T]].runtimeClass
    formatters += (clazz -> format)
    plugins += (clazz -> plugin)
  }

  override def readProjects(): Seq[ProjectConfig] = {
    for(projectDir <- file.listFiles.filter(_.isDirectory).toList) yield {
      val name = projectDir.getName
      val configXML = XML.load(res.child(name).get("config.xml").load)
      val prefixes = Prefixes.fromXML((configXML \ "Prefixes").head)
      ProjectConfig(name, prefixes)
    }
  }

  override def putProject(name: Identifier, config: ProjectConfig): Unit = {
    val configXMl =
      <ProjectConfig>
        { config.prefixes.toXML }
      </ProjectConfig>
    res.child(name).put("config.xml") { os => configXMl.write(os) }
  }

  override def deleteProject(name: Identifier): Unit = {
    res.delete(name)
  }

  override def readTasks[T: ClassTag](project: Identifier): Seq[T] = {
    plugin[T].loadTasks(res.child(project), res.child(project).child("resources")).values.toSeq
  }

  override def putTask[T: ClassTag](project: Identifier, data: T): Unit = {
    plugin[T].writeTask(data, res.child(project))
  }

  override def deleteTask[T: ClassTag](project: Identifier, task: Identifier): Unit = {
    plugin[T].removeTask(task, res.child(project))
  }

  private def formatter[T: ClassTag] = {
    formatters(implicitly[ClassTag[T]].runtimeClass).asInstanceOf[XmlFormat[T]]
  }

  private def plugin[T: ClassTag] = {
    plugins(implicitly[ClassTag[T]].runtimeClass).asInstanceOf[ModulePlugin[T]]
  }
}
