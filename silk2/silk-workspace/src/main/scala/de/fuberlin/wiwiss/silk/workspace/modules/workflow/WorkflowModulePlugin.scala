package de.fuberlin.wiwiss.silk.workspace.modules.workflow

import de.fuberlin.wiwiss.silk.config.{RuntimeConfig, LinkSpecification}
import de.fuberlin.wiwiss.silk.dataset.Dataset
import de.fuberlin.wiwiss.silk.execution.GenerateLinks
import de.fuberlin.wiwiss.silk.runtime.resource.{ResourceLoader, ResourceManager}
import de.fuberlin.wiwiss.silk.util.Identifier
import de.fuberlin.wiwiss.silk.util.XMLUtils._
import de.fuberlin.wiwiss.silk.workspace.Project
import de.fuberlin.wiwiss.silk.workspace.modules.linking.LinkingCaches
import de.fuberlin.wiwiss.silk.workspace.modules.{TaskActivity, Task, ModulePlugin}

import scala.xml.XML

class WorkflowModulePlugin extends ModulePlugin[Workflow] {

  override def prefix = "workflow"

  def createTask(name: Identifier, taskData: Workflow, project: Project): Task[Workflow] = {
    new Task(name, taskData, Nil, this, project)
  }

  /**
   * Loads all tasks of this module.
   */
  override def loadTasks(resources: ResourceLoader, project: Project): Seq[Task[Workflow]] = {
    val names = resources.list.filter(_.endsWith(".xml"))
    for(name <- names) yield {
      val xml = XML.load(resources.get(name).load)
      val workflow = Workflow.fromXML(xml, project)
      new Task(name.stripSuffix(".xml"), workflow, Nil, this, project)
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

  override def activities(task: Task[Workflow], project: Project): Seq[TaskActivity[_]] = {
    def workflowExecutor = new WorkflowExecutor(task.data.operators, project)

    TaskActivity(workflowExecutor) :: Nil
  }
}
