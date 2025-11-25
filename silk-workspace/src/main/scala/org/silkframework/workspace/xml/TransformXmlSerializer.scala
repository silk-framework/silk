package org.silkframework.workspace.xml

import org.silkframework.config._
import org.silkframework.rule.TransformSpec
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.resource.{ResourceLoader, ResourceManager}
import org.silkframework.runtime.serialization.XmlSerialization._
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Identifier
import org.silkframework.util.XMLUtils._
import org.silkframework.workspace.LoadedTask

import java.util.logging.Logger
import scala.util.control.NonFatal
import scala.xml._

/**
 * The transform module, which encapsulates all transform tasks.
 */
private class TransformXmlSerializer extends XmlSerializer[TransformSpec] {

  private val logger = Logger.getLogger(classOf[TransformXmlSerializer].getName)

  override def prefix = "transform"

  /**
   * Writes an updated task.
   */
  override def writeTask(data: Task[TransformSpec], resources: ResourceManager, projectResourceManager: ResourceManager): Unit = {
    val taskResources = resources.child(data.id)

    // Only serialize file paths correctly, paths should not be prefixed
    implicit val writeContext: WriteContext[Node] = WriteContext[Node](resources = projectResourceManager, prefixes = Prefixes.empty)

    val datasetXml = data.selection.toXML(asSource = true, writeContext.prefixes)
    val rulesXml = toXml(data)
    taskResources.get("dataset.xml").write() { os => datasetXml.write(os) }
    taskResources.get("rules.xml").write() { os => rulesXml.write(os) }
  }

  /**
   * Loads all tasks of this module.
   */
  override def loadTasks(resources: ResourceLoader)
                        (implicit context: PluginContext): Seq[LoadedTask[TransformSpec]] = {
    val tasks =
      for(name <- resources.listChildren) yield
        loadTask(name, resources.child(name))
    tasks
  }

  private def loadTask(name: Identifier, taskResources: ResourceLoader)
                      (implicit context: PluginContext): LoadedTask[TransformSpec] = {
    try {
      implicit val readContext = ReadContext.fromPluginContext()
      // Currently the transform spec is distributed in two xml files
      val datasetXml = loadResourceAsXml(taskResources, "dataset.xml")
      val rulesXml = loadResourceAsXml(taskResources, "rules.xml")
      var xml = rulesXml.copy(child = datasetXml ++ rulesXml.child)
      // Old XML versions do not contain the id
      if((xml \ "@id").isEmpty) {
        xml = xml % Attribute("id", Text(name), Null)
      }
      loadTaskSafelyFromXML(xml, resourceName = "rules.xml", alternativeTaskId = Some(name), taskResources)
    } catch {
      case NonFatal(ex) =>
        throw new ValidationException(s"Error loading task '$name': ${ex.getMessage}", ex)
    }
  }

  /**
   * Removes a specific task.
   */
  override def removeTask(name: Identifier, resources: ResourceManager): Unit = {
    resources.delete(name.toString)
  }
}