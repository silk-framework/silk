package org.silkframework.workspace.xml

import java.util.logging.{Level, Logger}

import org.silkframework.config._
import org.silkframework.runtime.resource.ResourceManager
import org.silkframework.util.Identifier
import org.silkframework.util.XMLUtils._
import org.silkframework.workspace.{ProjectConfig, RefreshableWorkspaceProvider, WorkspaceProvider}

import scala.reflect.ClassTag
import scala.util.control.NonFatal
import scala.xml.XML

/**
  * Holds all projects in a xml-based file structure.
  */
class XmlWorkspaceProvider(val resources: ResourceManager) extends WorkspaceProvider with RefreshableWorkspaceProvider {

  private val log = Logger.getLogger(classOf[XmlWorkspaceProvider].getName)

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
    resources.listChildren.flatMap(loadProject)
  }

  private def loadProject(projectName: String): Option[ProjectConfig] = {
    try {
      val configXML = XML.load(resources.child(projectName).get("config.xml").load)
      val prefixes = Prefixes.fromXML((configXML \ "Prefixes").head)
      val resourceURI = (configXML \ "@resourceUri").headOption.map(_.text.trim)
      Some(ProjectConfig(projectName, prefixes, resourceURI))
    } catch {
      case NonFatal(ex) =>
        log.log(Level.WARNING, s"Could not load project $projectName", ex)
        None
    }
  }

  override def putProject(config: ProjectConfig): Unit = {
    val uri = config.resourceUriOrElseDefaultUri
    val configXMl =
      <ProjectConfig resourceUri={uri}>
        { config.prefixes.toXML }
      </ProjectConfig>
    resources.child(config.id).get("config.xml").write { os => configXMl.write(os) }
  }

  override def deleteProject(name: Identifier): Unit = {
    resources.delete(name)
  }

  /**
    * Retrieves the project cache folder.
    */
  def projectCache(name: Identifier): ResourceManager = {
    resources.child(name)
  }

  override def readTasks[T <: TaskSpec : ClassTag](project: Identifier, projectResources: ResourceManager): Seq[Task[T]] = {
    plugin[T].loadTasks(resources.child(project).child(plugin[T].prefix), projectResources)
  }

  override def putTask[T <: TaskSpec : ClassTag](project: Identifier, task: Task[T]): Unit = {
    plugin[T].writeTask(task, resources.child(project).child(plugin[T].prefix))
  }

  override def deleteTask[T <: TaskSpec : ClassTag](project: Identifier, task: Identifier): Unit = {
    plugin[T].removeTask(task, resources.child(project).child(plugin[T].prefix))
  }

  private def plugin[T <: TaskSpec : ClassTag] = {
    val taskClass = implicitly[ClassTag[T]].runtimeClass
    plugins.find(_._1.isAssignableFrom(taskClass))
      .getOrElse(throw new RuntimeException("No plugin available for class " + taskClass))
      ._2.asInstanceOf[XmlSerializer[T]]
  }

  /**
    * Refreshes all projects, i.e. cleans all possible caches if there are any and reloads all projects freshly.
    */
  override def refresh(): Unit = {
    // No refresh needed, all tasks are read from the file system on every read. Nothing is cached
    // This is implemented to avoid warnings on project imports.
  }
}
