package de.fuberlin.wiwiss.silk.workspace.xml

import de.fuberlin.wiwiss.silk.runtime.resource.{ResourceLoader, ResourceManager}
import de.fuberlin.wiwiss.silk.util.Identifier
import de.fuberlin.wiwiss.silk.util.XMLUtils._
import de.fuberlin.wiwiss.silk.workspace.activity.workflow.Workflow

import scala.xml.XML

private class WorkflowXmlSerializer extends XmlSerializer[Workflow] {

  override def prefix = "workflow"

  /**
   * Loads all tasks of this module.
   */
  override def loadTasks(resources: ResourceLoader, projectResources: ResourceManager): Map[Identifier, Workflow] = {
    val names = resources.list.filter(_.endsWith(".xml"))
    val tasks =
      for(name <- names) yield {
        val xml = XML.load(resources.get(name).load)
        val identifier = Identifier(name.stripSuffix(".xml"))
        val workflow = Workflow.fromXML(xml)
        (identifier, workflow)
      }
    tasks.toMap
  }

  /**
   * Writes an updated task.
   */
  override def writeTask(data: Workflow, resources: ResourceManager): Unit = {
    resources.get(data.id + ".xml").write { os => data.toXML.write(os) }
  }

  /**
   * Removes a specific task.
   */
  override def removeTask(name: Identifier, resources: ResourceManager): Unit = {
    resources.delete(name + ".xml")
  }
}
