package de.fuberlin.wiwiss.silk.workspace

import de.fuberlin.wiwiss.silk.config.Prefixes
import de.fuberlin.wiwiss.silk.runtime.resource.ResourceManager
import de.fuberlin.wiwiss.silk.util.Identifier
import de.fuberlin.wiwiss.silk.util.XMLUtils._
import de.fuberlin.wiwiss.silk.workspace.modules.ModulePlugin
import scala.reflect.ClassTag
import scala.xml.XML

class FileWorkspaceProvider(res: ResourceManager) extends WorkspaceProvider {

  @volatile
  private var plugins = Map[Class[_], ModulePlugin[_]]()

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

  override def readTasks[T: ClassTag](project: Identifier): Seq[(Identifier, T)] = {
    plugin[T].loadTasks(res.child(project).child(plugin[T].prefix), res.child(project).child("resources")).toSeq
  }

  override def putTask[T: ClassTag](project: Identifier, data: T): Unit = {
    plugin[T].writeTask(data, res.child(project).child(plugin[T].prefix))
  }

  override def deleteTask[T: ClassTag](project: Identifier, task: Identifier): Unit = {
    plugin[T].removeTask(task, res.child(project).child(plugin[T].prefix))
  }

  private def plugin[T: ClassTag] = {
    plugins(implicitly[ClassTag[T]].runtimeClass).asInstanceOf[ModulePlugin[T]]
  }
}
