package de.fuberlin.wiwiss.silk.workspace.modules.transform

import java.util.logging.{Logger, Level}

import de.fuberlin.wiwiss.silk.config.{Dataset, Prefixes}
import de.fuberlin.wiwiss.silk.linkagerule.TransformRule
import de.fuberlin.wiwiss.silk.runtime.resource.{ResourceLoader, ResourceManager}
import de.fuberlin.wiwiss.silk.util.Identifier
import de.fuberlin.wiwiss.silk.workspace.Project
import de.fuberlin.wiwiss.silk.workspace.modules.ModuleProvider
import de.fuberlin.wiwiss.silk.util.XMLUtils._
import de.fuberlin.wiwiss.silk.workspace.modules.linking.LinkingCaches

import scala.xml.XML

/**
 * The transform module, which encapsulates all transform tasks.
 */
class TransformModuleProvider extends ModuleProvider[TransformConfig, TransformTask] {

  private val logger = Logger.getLogger(classOf[TransformModuleProvider].getName)

  /**
   * Loads the configuration for this module.
   */
  override def loadConfig(resources: ResourceLoader) = TransformConfig()

  /**
   * Writes updated configuration for this module.
   */
  override def writeConfig(config: TransformConfig, resources: ResourceManager): Unit = { }

  /**
   * Writes an updated task.
   */
  override def writeTask(task: TransformTask, resources: ResourceManager): Unit = {
    val taskResources = resources.child(task.name)

    //Don't use any prefixes
    implicit val prefixes = Prefixes.empty

    taskResources.put("dataset.xml") { os => task.dataset.toXML(asSource = true).write(os) }
    taskResources.put("rules.xml") { os =>
      <TransformRules>
      { task.rules.map(_.toXML) }
      </TransformRules>.write(os)
    }
    taskResources.put("cache.xml") { os => task.cache.toXML.write(os) }
  }

  /**
   * Loads all tasks of this module.
   */
  override def loadTasks(resources: ResourceLoader, project: Project): Seq[TransformTask] = {
    for(name <- resources.listChildren) yield
      loadTask(name, resources.child(name), project)
  }

  private def loadTask(name: String, taskResources: ResourceLoader, project: Project) = {
    implicit val prefixes = project.config.prefixes
    val dataset = Dataset.fromXML(XML.load(taskResources.get("dataset.xml").load))
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

    TransformTask(project, name, dataset, rules, cache)
  }

  /**
   * Removes a specific task.
   */
  override def removeTask(taskId: Identifier, resources: ResourceManager): Unit = {
    resources.delete(taskId)
  }
}