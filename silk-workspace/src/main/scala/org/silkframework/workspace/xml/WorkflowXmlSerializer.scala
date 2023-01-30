package org.silkframework.workspace.xml

import org.silkframework.config._
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.{ResourceLoader, ResourceManager}
import org.silkframework.runtime.serialization.ReadContext
import org.silkframework.runtime.serialization.XmlSerialization._
import org.silkframework.util.Identifier
import org.silkframework.util.XMLUtils._
import org.silkframework.workspace.LoadedTask
import org.silkframework.workspace.activity.workflow.Workflow

import scala.xml.{Attribute, Null, Text, XML}

private class WorkflowXmlSerializer extends XmlSerializer[Workflow] {

  override def prefix = "workflow"

  /**
   * Loads all tasks of this module.
   */
  override def loadTasks(resources: ResourceLoader, projectResources: ResourceManager)
                        (implicit user: UserContext): Seq[LoadedTask[Workflow]] = {
    implicit val readContext = ReadContext(projectResources, user = user)
    val names = resources.list.filter(_.endsWith(".xml"))
    val tasks =
      for(name <- names) yield {
        var xml = resources.get(name).read(XML.load)
        // Old XML versions do not contain the id
        if ((xml \ "@id").isEmpty) {
          xml = xml % Attribute("id", Text(name.stripSuffix(".xml")), Null)
        }
        loadTaskSafelyFromXML(xml, name, Some(name.stripSuffix(".xml")), resources, projectResources)
      }
    tasks
  }

  /**
   * Writes an updated task.
   */
  override def writeTask(task: Task[Workflow], resources: ResourceManager, projectResourceManager: ResourceManager): Unit = {
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
