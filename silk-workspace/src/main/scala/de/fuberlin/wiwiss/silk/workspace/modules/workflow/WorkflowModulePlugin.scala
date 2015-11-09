package de.fuberlin.wiwiss.silk.workspace.modules.workflow

import de.fuberlin.wiwiss.silk.runtime.resource.{ResourceLoader, ResourceManager}
import de.fuberlin.wiwiss.silk.util.Identifier
import de.fuberlin.wiwiss.silk.util.XMLUtils._
import de.fuberlin.wiwiss.silk.workspace.Project
import de.fuberlin.wiwiss.silk.workspace.modules.{ModulePlugin, Task, TaskActivity}

import scala.xml.XML

class WorkflowModulePlugin extends ModulePlugin[Workflow] {

  override def prefix = "workflow"

  /**
   * Loads all tasks of this module.
   */
  override def loadTasks(resources: ResourceLoader, projectResources: ResourceLoader): Map[Identifier, Workflow] = {
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

  override def activities(task: Task[Workflow], project: Project): Seq[TaskActivity[_,_]] = {
    def workflowExecutor = new WorkflowExecutor(task.data.operators, project)
    TaskActivity(workflowExecutor) :: Nil
  }
}
