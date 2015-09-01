package de.fuberlin.wiwiss.silk.workspace.modules.transform

import java.util.logging.Logger

import de.fuberlin.wiwiss.silk.config.{DatasetSelection, Prefixes, TransformSpecification}
import de.fuberlin.wiwiss.silk.dataset.Dataset
import de.fuberlin.wiwiss.silk.entity.EntityDescription
import de.fuberlin.wiwiss.silk.execution.ExecuteTransform
import de.fuberlin.wiwiss.silk.linkagerule.TransformRule
import de.fuberlin.wiwiss.silk.runtime.resource.{ResourceLoader, ResourceManager}
import de.fuberlin.wiwiss.silk.util.Identifier
import de.fuberlin.wiwiss.silk.util.XMLUtils._
import de.fuberlin.wiwiss.silk.workspace.Project
import de.fuberlin.wiwiss.silk.workspace.modules.{ModulePlugin, Task, TaskActivity}
import de.fuberlin.wiwiss.silk.runtime.serialization.Serialization._
import scala.xml.XML

/**
 * The transform module, which encapsulates all transform tasks.
 */
class TransformModulePlugin extends ModulePlugin[TransformSpecification] {

  private val logger = Logger.getLogger(classOf[TransformModulePlugin].getName)

  override def prefix = "transform"

  /**
   * Writes an updated task.
   */
  override def writeTask(data: TransformSpecification, resources: ResourceManager): Unit = {
    val taskResources = resources.child(data.id)

    //Don't use any prefixes
    implicit val prefixes = Prefixes.empty

    taskResources.put("dataset.xml") { os => data.selection.toXML(asSource = true).write(os) }
    taskResources.put("rules.xml") { os =>
      <TransformSpec>
        { data.rules.map(toXml[TransformRule]) }
        <Outputs>
        { data.outputs.map(o => <Output>{o}</Output>) }
        </Outputs>
      </TransformSpec>.write(os)
    }
  }

  /**
   * Loads all tasks of this module.
   */
  override def loadTasks(resources: ResourceLoader, projectResources: ResourceLoader): Map[Identifier, TransformSpecification] = {
    val tasks =
      for(name <- resources.listChildren) yield
        loadTask(name, resources.child(name), projectResources)
    tasks.toMap
  }

  private def loadTask(name: Identifier, taskResources: ResourceLoader, projectResources: ResourceLoader) = {
    implicit val resources = projectResources
    val dataset = DatasetSelection.fromXML(XML.load(taskResources.get("dataset.xml").load))
    val rulesXml = XML.load(taskResources.get("rules.xml").load)
    val rules = (rulesXml \ "TransformRule").map(fromXml[TransformRule])
    val outputs = (rulesXml \ "Outputs" \ "Output").map(_.text).map(Identifier(_))
    (name, TransformSpecification(name, dataset, rules, outputs))
  }

  /**
   * Removes a specific task.
   */
  override def removeTask(name: Identifier, resources: ResourceManager): Unit = {
    resources.delete(name)
  }

  override def activities(task: Task[TransformSpecification], project: Project): Seq[TaskActivity[_,_]] = {
    // Execute transform
    def executeTransform =
      new ExecuteTransform(
        input = project.task[Dataset](task.data.selection.datasetId).data.source,
        selection = task.data.selection,
        rules = task.data.rules,
        outputs = task.data.outputs.map(id => project.task[Dataset](id).data.sink)
      )
    def pathsCache() =
      new PathsCache(
        dataset = project.task[Dataset](task.data.selection.datasetId).data,
        transform = task.data
      )
    // Create task activities
    TaskActivity(executeTransform) ::
    TaskActivity("cache.xml", null: EntityDescription, pathsCache, project.resourceManager.child(prefix).child(task.name))  :: Nil
  }
}