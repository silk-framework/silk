package de.fuberlin.wiwiss.silk.workspace.modules.workflow

import de.fuberlin.wiwiss.silk.runtime.resource.{ResourceLoader, ResourceManager}
import de.fuberlin.wiwiss.silk.util.Identifier
import de.fuberlin.wiwiss.silk.workspace.Project
import de.fuberlin.wiwiss.silk.workspace.modules.ModuleProvider

class WorkflowModuleProvider extends ModuleProvider[WorkflowTask] {

  override def prefix = "workflow"

  /**
   * Loads all tasks of this module.
   */
  override def loadTasks(resources: ResourceLoader, project: Project): Seq[WorkflowTask] = {
    Seq(new WorkflowTask("workflow"))
  }

  /**
   * Writes an updated task.
   */
  override def writeTask(task: WorkflowTask, resources: ResourceManager): Unit = {
    //resources.put(task.name + ".xml"){ os => <WorkflowTask/>.write(os) }
  }

  /**
   * Removes a specific task.
   */
  override def removeTask(taskId: Identifier, resources: ResourceManager): Unit = {
    //resources.delete(taskId + ".xml")
  }
}
