package de.fuberlin.wiwiss.silk.workspace.io

import de.fuberlin.wiwiss.silk.config.{LinkSpecification, TransformSpecification}
import de.fuberlin.wiwiss.silk.dataset.Dataset
import de.fuberlin.wiwiss.silk.runtime.resource.ResourceManager
import de.fuberlin.wiwiss.silk.util.Identifier
import de.fuberlin.wiwiss.silk.workspace.{ProjectConfig, WorkspaceProvider}
import de.fuberlin.wiwiss.silk.workspace.activity.workflow.Workflow

import scala.reflect.ClassTag

/**
  * Transfers projects between workspaces.
  */
object WorkspaceIO {

  /**
    * Copies all projects in one workspace to another workspace.
    */
  def copyProjects(inputWorkspace: WorkspaceProvider, outputWorkspace: WorkspaceProvider): Unit = {
    for(project <- inputWorkspace.readProjects()) {
      copyProject(inputWorkspace, outputWorkspace, project)
    }
  }

  /**
    * Copies a project from one workspace to another workspace
    */
  def copyProject(inputWorkspace: WorkspaceProvider, outputWorkspace: WorkspaceProvider, project: ProjectConfig): Unit = {
    outputWorkspace.putProject(project)
    copyResources(inputWorkspace.projectResources(project.id), outputWorkspace.projectResources(project.id))
    copyTasks[Dataset](inputWorkspace, outputWorkspace, project.id)
    copyTasks[TransformSpecification](inputWorkspace, outputWorkspace, project.id)
    copyTasks[LinkSpecification](inputWorkspace, outputWorkspace, project.id)
    copyTasks[Workflow](inputWorkspace, outputWorkspace, project.id)
  }

  private def copyResources(inputResources: ResourceManager, outputResources: ResourceManager): Unit = {
    // Copy resources at the current path
    for(resourceName <- inputResources.list) {
      val input = inputResources.get(resourceName)
      val output = outputResources.get(resourceName, mustExist = false)
      output.write(input.load)
    }
    // Copy child resources recursively
    for(childName <- inputResources.listChildren) {
      copyResources(inputResources.child(childName), outputResources.child(childName))
    }
  }

  private def copyTasks[T: ClassTag](inputWorkspace: WorkspaceProvider, outputWorkspace: WorkspaceProvider, projectName: Identifier): Unit = {
    for((taskName, taskData) <- inputWorkspace.readTasks[T](projectName)) {
      outputWorkspace.putTask(projectName, taskName, taskData)
    }
  }

}
