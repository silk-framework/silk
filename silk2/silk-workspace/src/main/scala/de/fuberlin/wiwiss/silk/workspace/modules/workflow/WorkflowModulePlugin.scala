package de.fuberlin.wiwiss.silk.workspace.modules.workflow

import de.fuberlin.wiwiss.silk.config.LinkSpecification
import de.fuberlin.wiwiss.silk.runtime.resource.{ResourceLoader, ResourceManager}
import de.fuberlin.wiwiss.silk.util.Identifier
import de.fuberlin.wiwiss.silk.util.XMLUtils._
import de.fuberlin.wiwiss.silk.workspace.Project
import de.fuberlin.wiwiss.silk.workspace.modules.linking.LinkingCaches
import de.fuberlin.wiwiss.silk.workspace.modules.{Task, ModulePlugin}

import scala.xml.XML

class WorkflowModulePlugin extends ModulePlugin[Workflow] {

  override def prefix = "workflow"

  def createTask(name: Identifier, taskData: Workflow, project: Project): Task[Workflow] = {
    new Task(name, taskData, Seq.empty, project)
  }

  /**
   * Loads all tasks of this module.
   */
  override def loadTasks(resources: ResourceLoader, project: Project): Seq[Task[Workflow]] = {
    val names = resources.list.filter(_.endsWith(".xml"))
    for(name <- names) yield {
      val xml = XML.load(resources.get(name).load)
      val workflow = Workflow.fromXML(xml, project)
      new Task(name.stripSuffix(".xml"), workflow, Nil, project)
    }
  }

  /**
   * Writes an updated task.
   */
  override def writeTask(task: Task[Workflow], resources: ResourceManager): Unit = {
    resources.put(task.name + ".xml"){ os => task.data.toXML.write(os) }
  }

  /**
   * Removes a specific task.
   */
  override def removeTask(taskId: Identifier, resources: ResourceManager): Unit = {
    resources.delete(taskId + ".xml")
  }
}
