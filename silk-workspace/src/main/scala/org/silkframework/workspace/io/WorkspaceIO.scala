package org.silkframework.workspace.io

import java.util.logging.Logger

import org.silkframework.config.{CustomTask, TaskSpec}
import org.silkframework.dataset.{Dataset, DatasetSpec}
import org.silkframework.rule.{LinkSpec, TransformSpec}
import org.silkframework.runtime.resource.{EmptyResourceManager, ResourceManager}
import org.silkframework.util.Identifier
import org.silkframework.workspace.activity.workflow.Workflow
import org.silkframework.workspace.resources.ResourceRepository
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
  def copyProjects(inputWorkspace: WorkspaceProvider, outputWorkspace: WorkspaceProvider,
                   inputResources: Option[ResourceRepository], outputResources: Option[ResourceRepository]): Unit = {
    for(project <- inputWorkspace.readProjects()) {
      copyProject(inputWorkspace, outputWorkspace,
        inputResources.map(_.get(project.id)), outputResources.map(_.get(project.id)), project)
    }
  }

  /**
    * Copies a project from one workspace to another workspace.
    */
  def copyProject(inputWorkspace: WorkspaceProvider, outputWorkspace: WorkspaceProvider,
                  inputResources: Option[ResourceManager], outputResources: Option[ResourceManager],
                  project: ProjectConfig): Unit = {
    val updatedProjectConfig = project.copy(projectResourceUriOpt = Some(project.resourceUriOrElseDefaultUri))
    outputWorkspace.putProject(updatedProjectConfig)
    for(input <- inputResources; output <- outputResources)
      copyResources(input, output)
    val resources = outputResources.getOrElse(inputResources.getOrElse(EmptyResourceManager))
    copyTasks[DatasetSpec](inputWorkspace, outputWorkspace, resources, updatedProjectConfig.id)
    copyTasks[TransformSpec](inputWorkspace, outputWorkspace, resources, updatedProjectConfig.id)
    copyTasks[LinkSpec](inputWorkspace, outputWorkspace, resources, updatedProjectConfig.id)
    copyTasks[Workflow](inputWorkspace, outputWorkspace, resources, updatedProjectConfig.id)
    copyTasks[CustomTask](inputWorkspace, outputWorkspace, resources, updatedProjectConfig.id)
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
      output.writeResource(input)
    }
    // Copy child resources recursively
    for(childName <- inputResources.listChildren) {
      copyResources(inputResources.child(childName), outputResources.child(childName))
    }
  }

  private def copyTasks[T <: TaskSpec : ClassTag](inputWorkspace: WorkspaceProvider, outputWorkspace: WorkspaceProvider, resources: ResourceManager, projectName: Identifier): Unit = {
    for(task <- inputWorkspace.readTasks[T](projectName, resources)) {
      outputWorkspace.putTask(projectName, task)
    }
  }

}
