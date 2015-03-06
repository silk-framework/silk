package de.fuberlin.wiwiss.silk.workspace.modules.transform

import java.util.logging.{Logger, Level}

import de.fuberlin.wiwiss.silk.config.{LinkSpecification, TransformSpecification, DatasetSelection, Prefixes}
import de.fuberlin.wiwiss.silk.linkagerule.TransformRule
import de.fuberlin.wiwiss.silk.runtime.resource.{ResourceLoader, ResourceManager}
import de.fuberlin.wiwiss.silk.util.Identifier
import de.fuberlin.wiwiss.silk.workspace.Project
import de.fuberlin.wiwiss.silk.workspace.modules.{Task, ModulePlugin}
import de.fuberlin.wiwiss.silk.util.XMLUtils._
import de.fuberlin.wiwiss.silk.workspace.modules.linking.LinkingCaches

import scala.xml.XML

/**
 * The transform module, which encapsulates all transform tasks.
 */
class TransformModulePlugin extends ModulePlugin[TransformSpecification] {

  private val logger = Logger.getLogger(classOf[TransformModulePlugin].getName)

  override def prefix = "transform"

  def createTask(name: Identifier, taskData: TransformSpecification, project: Project): Task[TransformSpecification] = {
    new Task(name, taskData, Seq(new PathsCache()), project)
  }

  /**
   * Writes an updated task.
   */
  override def writeTask(task: Task[TransformSpecification], resources: ResourceManager): Unit = {
    val taskResources = resources.child(task.name)

    //Don't use any prefixes
    implicit val prefixes = Prefixes.empty

    taskResources.put("dataset.xml") { os => task.data.selection.toXML(asSource = true).write(os) }
    taskResources.put("rules.xml") { os =>
      <TransformRules>
      { task.data.rules.map(_.toXML) }
      </TransformRules>.write(os)
    }
    taskResources.put("cache.xml") { os => task.caches.head.toXML.write(os) }
  }

  /**
   * Loads all tasks of this module.
   */
  override def loadTasks(resources: ResourceLoader, project: Project): Seq[Task[TransformSpecification]] = {
    for(name <- resources.listChildren) yield
      loadTask(name, resources.child(name), project)
  }

  private def loadTask(name: String, taskResources: ResourceLoader, project: Project): Task[TransformSpecification] = {
    implicit val prefixes = project.config.prefixes
    val dataset = DatasetSelection.fromXML(XML.load(taskResources.get("dataset.xml").load))
    val rulesXml = XML.load(taskResources.get("rules.xml").load)
    val rules = (rulesXml \ "TransformRule").map(TransformRule.load(project.resources)(project.config.prefixes))
    val cache = new PathsCache()

    //Load the cache
    try {
      cache.loadFromXML(XML.load(taskResources.get("cache.xml").load))
    } catch {
      case ex : Exception =>
        logger.log(Level.WARNING, "Cache corrupted. Rebuilding Cache.", ex)
        new LinkingCaches()
    }

    new Task(name, TransformSpecification(name, dataset, rules), Seq(cache), project)
  }

  /**
   * Removes a specific task.
   */
  override def removeTask(taskId: Identifier, resources: ResourceManager): Unit = {
    resources.delete(taskId)
  }
}