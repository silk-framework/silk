package de.fuberlin.wiwiss.silk.workspace.modules.workflow

import de.fuberlin.wiwiss.silk.runtime.resource.{ResourceLoader, ResourceManager}
import de.fuberlin.wiwiss.silk.util.Identifier
import de.fuberlin.wiwiss.silk.util.XMLUtils._
import de.fuberlin.wiwiss.silk.workspace.Project
import de.fuberlin.wiwiss.silk.workspace.modules.ModulePlugin

import scala.xml.XML

class WorkflowModulePlugin extends ModulePlugin[WorkflowTask] {

  override def prefix = "workflow"

  /**
   * Loads all tasks of this module.
   */
  override def loadTasks(resources: ResourceLoader, project: Project): Seq[WorkflowTask] = {
    val names = resources.list.filter(_.endsWith(".xml"))
    for(name <- names) yield {
      val xml = XML.load(resources.get(name).load)
      WorkflowTask.fromXML(name.stripSuffix(".xml"), xml, project)
    }
  }

  /**
   * Writes an updated task.
   */
  override def writeTask(task: WorkflowTask, resources: ResourceManager): Unit = {
    resources.put(task.name + ".xml"){ os => task.toXML.write(os) }
  }

  /**
   * Removes a specific task.
   */
  override def removeTask(taskId: Identifier, resources: ResourceManager): Unit = {
    resources.delete(taskId + ".xml")
  }
}
