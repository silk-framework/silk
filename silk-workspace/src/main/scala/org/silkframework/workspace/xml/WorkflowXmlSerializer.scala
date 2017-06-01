package org.silkframework.workspace.xml

import org.silkframework.config._
import org.silkframework.runtime.resource.{ResourceLoader, ResourceManager}
import org.silkframework.runtime.serialization.ReadContext
import org.silkframework.runtime.serialization.XmlSerialization.fromXml
import org.silkframework.util.Identifier
import org.silkframework.util.XMLUtils._
import org.silkframework.workspace.activity.workflow.Workflow
import org.silkframework.runtime.serialization.XmlSerialization._

import scala.xml.XML

private class WorkflowXmlSerializer extends XmlSerializer[Workflow] {

  override def prefix = "workflow"

  /**
   * Loads all tasks of this module.
   */
  override def loadTasks(resources: ResourceLoader, projectResources: ResourceManager): Seq[Task[Workflow]] = {
    implicit val readContext = ReadContext(projectResources)
    val names = resources.list.filter(_.endsWith(".xml"))
    val tasks =
      for(name <- names) yield {
        val xml = XML.load(resources.get(name).load)
        fromXml[Task[Workflow]](xml)
      }
    tasks
  }

  /**
   * Writes an updated task.
   */
  override def writeTask(task: Task[Workflow], resources: ResourceManager): Unit = {
    resources.get(task.id.toString + ".xml").write { os => toXml(task).write(os) }
  }

  /**
   * Removes a specific task.
   */
  override def removeTask(name: Identifier, resources: ResourceManager): Unit = {
    resources.delete(name.toString + ".xml")
  }
}
