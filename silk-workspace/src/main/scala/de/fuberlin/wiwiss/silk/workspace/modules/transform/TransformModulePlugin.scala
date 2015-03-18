package de.fuberlin.wiwiss.silk.workspace.modules.transform

import java.util.logging.{Level, Logger}

import de.fuberlin.wiwiss.silk.config.{DatasetSelection, Prefixes, TransformSpecification}
import de.fuberlin.wiwiss.silk.dataset.Dataset
import de.fuberlin.wiwiss.silk.execution.ExecuteTransform
import de.fuberlin.wiwiss.silk.linkagerule.TransformRule
import de.fuberlin.wiwiss.silk.runtime.resource.{ResourceLoader, ResourceManager}
import de.fuberlin.wiwiss.silk.util.Identifier
import de.fuberlin.wiwiss.silk.util.XMLUtils._
import de.fuberlin.wiwiss.silk.workspace.Project
import de.fuberlin.wiwiss.silk.workspace.modules.linking.LinkingCaches
import de.fuberlin.wiwiss.silk.workspace.modules.{ModulePlugin, Task, TaskActivity}

import scala.xml.XML

/**
 * The transform module, which encapsulates all transform tasks.
 */
class TransformModulePlugin extends ModulePlugin[TransformSpecification] {

  private val logger = Logger.getLogger(classOf[TransformModulePlugin].getName)

  override def prefix = "transform"

  def createTask(name: Identifier, taskData: TransformSpecification, project: Project): Task[TransformSpecification] = {
    new Task(name, taskData, Seq(new PathsCache()), this, project)
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
      <TransformSpec>
      { task.data.rules.map(_.toXML) }
      { task.data.outputs.map(_.toXML) }
      </TransformSpec>.write(os)
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
    val outputs = (rulesXml \ "Dataset").map(Dataset.fromXML(_, taskResources))
    val cache = new PathsCache()

    //Load the cache
    try {
      cache.loadFromXML(XML.load(taskResources.get("cache.xml").load))
    } catch {
      case ex : Exception =>
        logger.log(Level.WARNING, "Cache corrupted. Rebuilding Cache.", ex)
        new LinkingCaches()
    }

    new Task(name, TransformSpecification(name, dataset, rules, outputs), Seq(cache), this, project)
  }

  /**
   * Removes a specific task.
   */
  override def removeTask(taskId: Identifier, resources: ResourceManager): Unit = {
    resources.delete(taskId)
  }

  override def activities(task: Task[TransformSpecification], project: Project): Seq[TaskActivity[_]] = {
    // Execute transform
    def executeTransform =
      new ExecuteTransform(
        input = project.task[Dataset](task.data.selection.datasetId).data.source,
        selection = task.data.selection,
        rules = task.data.rules,
        outputs = task.data.outputs.map(_.sink)
      )
    // Create task activities
    TaskActivity(executeTransform) :: Nil
  }
}