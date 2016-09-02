package org.silkframework.workspace.xml

import java.io._
import java.util.zip.{ZipEntry, ZipInputStream, ZipOutputStream}

import org.silkframework.config._
import org.silkframework.runtime.resource.{ResourceLoader, ResourceManager}
import org.silkframework.util.Identifier
import org.silkframework.util.XMLUtils._
import org.silkframework.workspace.{ProjectConfig, WorkspaceProvider}

import scala.reflect.ClassTag
import scala.xml.XML

/**
  * Holds all projects in a xml-based file structure.
  */
class XmlWorkspaceProvider(res: ResourceManager) extends WorkspaceProvider {

  @volatile
  private var plugins = Map[Class[_], XmlSerializer[_]]()

  // Register all module types
  registerModule(new DatasetXmlSerializer())
  registerModule(new LinkingXmlSerializer())
  registerModule(new TransformXmlSerializer())
  registerModule(new WorkflowXmlSerializer())
  registerModule(new CustomTaskXmlSerializer())

  private def registerModule[T <: TaskSpec : ClassTag](plugin: XmlSerializer[T]) = {
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

  override def readTasks[T <: TaskSpec : ClassTag](project: Identifier): Seq[(Identifier, T)] = {
    plugin[T].loadTasks(res.child(project).child(plugin[T].prefix), res.child(project).child("resources")).toSeq
  }

  override def putTask[T <: TaskSpec : ClassTag](project: Identifier, task: Identifier, data: T): Unit = {
    plugin[T].writeTask(PlainTask(task, data), res.child(project).child(plugin[T].prefix))
  }

  override def deleteTask[T <: TaskSpec : ClassTag](project: Identifier, task: Identifier): Unit = {
    plugin[T].removeTask(task, res.child(project).child(plugin[T].prefix))
  }

  private def plugin[T <: TaskSpec : ClassTag] = {
    plugins(implicitly[ClassTag[T]].runtimeClass).asInstanceOf[XmlSerializer[T]]
  }
}
