package org.silkframework.workspace.io

import org.silkframework.config.{CustomTask, Prefixes, TaskSpec}
import org.silkframework.dataset.{Dataset, DatasetSpec}
import org.silkframework.rule.{LinkSpec, TransformSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.resource.ResourceManager
import org.silkframework.runtime.templating.{CombinedTemplateVariablesReader, GlobalTemplateVariables, InMemoryTemplateVariablesReader, TemplateVariables}
import org.silkframework.util.Identifier
import org.silkframework.workspace.activity.workflow.Workflow
import org.silkframework.workspace.resources.ResourceRepository
import org.silkframework.workspace.{ProjectConfig, WorkspaceProvider}

import java.util.logging.Logger
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
                   inputResources: ResourceRepository, outputResources: ResourceRepository,
                   alsoCopyResources: Boolean = true)
                  (implicit userContext: UserContext): Unit = {
    for(project <- inputWorkspace.readProjects()) {
      copyProject(inputWorkspace, outputWorkspace, inputResources.get(project.id), outputResources.get(project.id), project, alsoCopyResources)
    }
  }

  /**
    * Copies a project from one workspace to another workspace.
    */
  def copyProject(inputWorkspace: WorkspaceProvider, outputWorkspace: WorkspaceProvider,
                  inputResources: ResourceManager, outputResources: ResourceManager,
                  project: ProjectConfig, alsoCopyResources: Boolean = true)
                 (implicit userContext: UserContext): Unit = {
    val updatedProjectConfig = project.copy(projectResourceUriOpt = Some(project.resourceUriOrElseDefaultUri))
    outputWorkspace.putProject(updatedProjectConfig)
    // Variables
    val variables = inputWorkspace.projectVariables(updatedProjectConfig.id).readVariables()
    outputWorkspace.projectVariables(updatedProjectConfig.id).putVariables(variables)
    // Tags
    val tags = inputWorkspace.readTags(updatedProjectConfig.id)
    outputWorkspace.putTags(updatedProjectConfig.id, tags)
    // Resources
    if(alsoCopyResources) {
      copyResources(inputResources, outputResources)
    }
    // Tasks
    copyTasks[DatasetSpec[Dataset]](inputWorkspace, outputWorkspace, inputResources, outputResources, updatedProjectConfig.id, project.prefixes, variables)
    copyTasks[TransformSpec](inputWorkspace, outputWorkspace, inputResources, outputResources, updatedProjectConfig.id, project.prefixes, variables)
    copyTasks[LinkSpec](inputWorkspace, outputWorkspace, inputResources, outputResources, updatedProjectConfig.id, project.prefixes, variables)
    copyTasks[Workflow](inputWorkspace, outputWorkspace, inputResources, outputResources, updatedProjectConfig.id, project.prefixes, variables)
    copyTasks[CustomTask](inputWorkspace, outputWorkspace, inputResources, outputResources, updatedProjectConfig.id, project.prefixes, variables)
    outputWorkspace.refreshProject(updatedProjectConfig.id, outputResources)
  }

  def copyResources(inputResources: ResourceManager, outputResources: ResourceManager): Unit = {
    // Copy resources at the current path
    for(resourceName <- inputResources.list) {
      val input = inputResources.get(resourceName)
      val output = outputResources.get(resourceName, mustExist = false)
      if(inputResources.basePath != outputResources.basePath || !output.exists) {
        output.writeResource(input)
      }
    }
    // Copy child resources recursively
    for(childName <- inputResources.listChildren) {
      copyResources(inputResources.child(childName), outputResources.child(childName))
    }
  }

  private def copyTasks[T <: TaskSpec : ClassTag](inputWorkspace: WorkspaceProvider,
                                                  outputWorkspace: WorkspaceProvider,
                                                  inputResources: ResourceManager,
                                                  outputResources: ResourceManager,
                                                  projectName: Identifier,
                                                  prefixes: Prefixes,
                                                  variables: TemplateVariables)
                                                 (implicit userContext: UserContext): Unit = {
    val variablesReader = CombinedTemplateVariablesReader(Seq(GlobalTemplateVariables, InMemoryTemplateVariablesReader(variables, Set("project"))))
    implicit val inputContext: PluginContext = PluginContext(resources = inputResources, prefixes = prefixes, user = userContext, templateVariables = variablesReader)
    for(taskTry <- inputWorkspace.readTasks[T](projectName)) {
      taskTry.taskOrError match {
        case Right(task) =>
          outputWorkspace.putTask(projectName, task, inputResources)
        case Left(taskLoadingError) =>
          outputWorkspace.retainExternalTaskLoadingError(projectName, taskLoadingError)
          log.warning("Invalid task encountered while copying task between workspace providers. Error message: " + taskLoadingError.throwable.getMessage)
      }
    }
  }

}
