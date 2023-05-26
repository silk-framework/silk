package org.silkframework.workspace.xml

import org.silkframework.config._
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.resource.{ResourceLoader, ResourceManager}
import org.silkframework.runtime.serialization.WriteContext
import org.silkframework.runtime.serialization.XmlSerialization._
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Identifier
import org.silkframework.util.XMLUtils._
import org.silkframework.workspace.LoadedTask
import org.silkframework.workspace.activity.workflow.Workflow

import scala.util.control.NonFatal
import scala.xml._

private class WorkflowXmlSerializer extends XmlSerializer[Workflow] {

  override def prefix = "workflow"



  /**
   * Loads all tasks of this module.
   */
  override def loadTasks(resources: ResourceLoader)
                        (implicit context: PluginContext): Seq[LoadedTask[Workflow]] = {
    val names = resources.list.filter(_.endsWith(".xml"))
    for(name <- names) yield {
      loadTask(name, resources)
    }
  }

  private def loadTask(name: String, resources: ResourceLoader)
                      (implicit context: PluginContext): LoadedTask[Workflow] = {
    try {
      var xml = loadResourceAsXml(resources, name)
      // Old XML versions do not contain the id
      if ((xml \ "@id").isEmpty) {
        xml = xml % Attribute("id", Text(name.stripSuffix(".xml")), Null)
      }
      loadTaskSafelyFromXML(xml, name, Some(name.stripSuffix(".xml")), resources)
    } catch {
      case NonFatal(ex) =>
        throw new ValidationException(s"Error loading task '$name': ${ex.getMessage}", ex)
    }
  }

  /**
   * Writes an updated task.
   */
  override def writeTask(task: Task[Workflow], resources: ResourceManager, projectResourceManager: ResourceManager): Unit = {
    // Only serialize file paths correctly, paths should not be prefixed
    implicit val writeContext: WriteContext[Node] = WriteContext[Node](resources = projectResourceManager, prefixes = Prefixes.empty)
    val workflowXml = toXml(task)
    resources.get(task.id.toString + ".xml").write() { os => workflowXml.write(os) }
  }

  /**
   * Removes a specific task.
   */
  override def removeTask(name: Identifier, resources: ResourceManager): Unit = {
    resources.delete(name.toString + ".xml")
  }
}
