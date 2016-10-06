package org.silkframework.workspace.io

import java.util.logging.Logger

import org.silkframework.config.{CustomTask, TaskSpec}
import org.silkframework.dataset.Dataset
import org.silkframework.rule.{LinkSpec, TransformSpec}
import org.silkframework.runtime.resource.ResourceManager
import org.silkframework.util.Identifier
import org.silkframework.workspace.activity.workflow.Workflow
import org.silkframework.workspace.{ProjectConfig, RefreshableWorkspaceProvider, WorkspaceProvider}

import scala.reflect.ClassTag

/**
  * Transfers projects between workspaces.
  */
object WorkspaceIO {
  private lazy val log: Logger = Logger.getLogger(this.getClass.getName)

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
    val updatedProjectConfig = project.copy(projectResourceUriOpt = Some(project.resourceUriOrElseDefaultUri))
    outputWorkspace.putProject(updatedProjectConfig)
    copyResources(inputWorkspace.projectResources(updatedProjectConfig.id), outputWorkspace.projectResources(updatedProjectConfig.id))
    copyTasks[Dataset](inputWorkspace, outputWorkspace, updatedProjectConfig.id)
    copyTasks[TransformSpec](inputWorkspace, outputWorkspace, updatedProjectConfig.id)
    copyTasks[LinkSpec](inputWorkspace, outputWorkspace, updatedProjectConfig.id)
    copyTasks[Workflow](inputWorkspace, outputWorkspace, updatedProjectConfig.id)
    copyTasks[CustomTask](inputWorkspace, outputWorkspace, updatedProjectConfig.id)
    outputWorkspace match {
      case rw: RefreshableWorkspaceProvider =>
        rw.refresh()
      case _ =>
        log.warning("Workspace provider of type " + outputWorkspace.getClass.getName + " is not refreshable. Imported project " +
        updatedProjectConfig.id.toString + " might be inconsistent.")
    }
  }

  def copyResources(inputResources: ResourceManager, outputResources: ResourceManager): Unit = {
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

  private def copyTasks[T <: TaskSpec : ClassTag](inputWorkspace: WorkspaceProvider, outputWorkspace: WorkspaceProvider, projectName: Identifier): Unit = {
    for((taskName, taskData) <- inputWorkspace.readTasks[T](projectName)) {
      outputWorkspace.putTask(projectName, taskName, taskData)
    }
  }

}
