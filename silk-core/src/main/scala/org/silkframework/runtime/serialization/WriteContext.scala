package org.silkframework.runtime.serialization

import org.silkframework.config.Prefixes
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.resource.{EmptyResourceManager, ResourceManager}
import org.silkframework.runtime.templating.{GlobalTemplateVariables, TemplateVariablesReader}
import org.silkframework.util.Identifier
import org.silkframework.workspace.ProjectTrait

/**
  * Holds context information when serializing data.
  */
case class WriteContext[U](parent: Option[U] = None,
                           prefixes: Prefixes,
                           projectId: Option[Identifier] = None,
                           projectUri: Option[String] = None,
                           resources: ResourceManager = EmptyResourceManager(),
                           user: UserContext = UserContext.Empty,
                           templateVariables: TemplateVariablesReader = GlobalTemplateVariables,
                           workflowId: Option[Identifier] = None) extends PluginContext

object WriteContext {

  def empty[U]: WriteContext[U] = WriteContext(resources = EmptyResourceManager(), user = UserContext.Empty, prefixes = Prefixes.empty)

  def fromProject[U](project: ProjectTrait, parent: Option[U] = None)(implicit user: UserContext): WriteContext[U] = {
    WriteContext[U](
      parent = parent,
      prefixes = project.config.prefixes,
      projectId = Some(project.id),
      projectUri = project.config.projectResourceUriOpt,
      resources = project.resources,
      user = user,
      templateVariables = project.combinedTemplateVariables
    )
  }

  def fromPluginContext[U](parent: Option[U] = None,
                           projectUri: Option[String] = None)(implicit pluginContext: PluginContext): WriteContext[U] = {
    WriteContext[U](
      parent = parent,
      prefixes = pluginContext.prefixes,
      projectId = pluginContext.projectId,
      projectUri = projectUri,
      resources = pluginContext.resources,
      user = pluginContext.user,
      templateVariables = pluginContext.templateVariables,
      workflowId = pluginContext.workflowId
    )
  }
}
