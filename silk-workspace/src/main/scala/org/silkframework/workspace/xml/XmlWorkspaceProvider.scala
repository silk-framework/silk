package org.silkframework.workspace.xml

import java.util.logging.{Level, Logger}

import org.silkframework.config._
import org.silkframework.dataset.rdf.SparqlEndpoint
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.ResourceManager
import org.silkframework.runtime.serialization.{ReadContext, XmlSerialization}
import org.silkframework.util.{Identifier, XMLUtils}
import org.silkframework.util.XMLUtils._
import org.silkframework.workspace.{ProjectConfig, TaskLoadingError, WorkspaceProvider}

import scala.reflect.ClassTag
import scala.util.Try
import scala.util.control.NonFatal
import scala.xml.{Elem, XML}

/**
  * Holds all projects in a xml-based file structure.
  */
class XmlWorkspaceProvider(val resources: ResourceManager) extends WorkspaceProvider {

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

  override def readProjects()
                           (implicit userContext: UserContext): Seq[ProjectConfig] = {
    resources.listChildren.flatMap(readProject(_))
  }

  override def readProject(projectName: String)
                          (implicit userContext: UserContext): Option[ProjectConfig] = {
    try {
      val configXML = resources.child(projectName).get("config.xml").read(XML.load)
      val prefixes = Prefixes.fromXML((configXML \ "Prefixes").head)
      val resourceURI = (configXML \ "@resourceUri").headOption.map(_.text.trim)
      Some(ProjectConfig(projectName, prefixes, resourceURI, metaData(configXML, projectName)))
    } catch {
      case NonFatal(ex) =>
        log.log(Level.WARNING, s"Could not load project $projectName", ex)
        None
    }
  }

  private def metaData(configXML: Elem,
                       projectName: String): MetaData = {
    implicit val readContext: ReadContext = ReadContext()
    (configXML \ "MetaData").headOption.
        map(n => XmlSerialization.fromXml[MetaData](n)).
        getOrElse(MetaData(projectName)) // Set label to ID
  }

  override def putProject(config: ProjectConfig)
                         (implicit userContext: UserContext): Unit = {
    val uri = config.resourceUriOrElseDefaultUri
    val configXMl =
      <ProjectConfig resourceUri={uri}>
        { config.prefixes.toXML }
        { XmlSerialization.toXml(config.metaData) }
      </ProjectConfig>
    resources.child(config.id).get("config.xml").write(){ os => configXMl.write(os) }
  }

  override def deleteProject(name: Identifier)
                            (implicit userContext: UserContext): Unit = {
    resources.delete(name)
  }

  /**
    * Retrieves the project cache folder.
    */
  def projectCache(name: Identifier): ResourceManager = {
    resources.child(name)
  }

  override def readTasks[T <: TaskSpec : ClassTag](project: Identifier, projectResources: ResourceManager)
                                                  (implicit userContext: UserContext): Seq[Task[T]] = {
    plugin[T].loadTasks(resources.child(project).child(plugin[T].prefix), projectResources)
  }

  override def putTask[T <: TaskSpec : ClassTag](project: Identifier, task: Task[T])
                                                (implicit userContext: UserContext): Unit = {
    plugin[T].writeTask(task, resources.child(project).child(plugin[T].prefix))
  }

  override def deleteTask[T <: TaskSpec : ClassTag](project: Identifier, task: Identifier)
                                                   (implicit userContext: UserContext): Unit = {
    plugin[T].removeTask(task, resources.child(project).child(plugin[T].prefix))
  }

  private def plugin[T <: TaskSpec : ClassTag]: XmlSerializer[T] = {
    val taskClass = implicitly[ClassTag[T]].runtimeClass
    plugins.find(_._1.isAssignableFrom(taskClass))
      .getOrElse(throw new RuntimeException("No plugin available for class " + taskClass))
      ._2.asInstanceOf[XmlSerializer[T]]
  }

  /**
    * Refreshes all projects, i.e. cleans all possible caches if there are any and reloads all projects freshly.
    */
  override def refresh()(implicit userContext: UserContext): Unit = {
    // No refresh needed, all tasks are read from the file system on every read. Nothing is cached
    // This is implemented to avoid warnings on project imports.
  }

  override def readTasksSafe[T <: TaskSpec : ClassTag](project: Identifier,
                                                       projectResources: ResourceManager)
                                                      (implicit user: UserContext): Seq[Either[Task[T], TaskLoadingError]] = {
    plugin[T].loadTasksSafe(resources.child(project).child(plugin[T].prefix), projectResources)
  }

  /**
    * Returns None, because the projects are not held as RDF.
    */
  override def sparqlEndpoint: Option[SparqlEndpoint] = None
}
