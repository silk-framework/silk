package org.silkframework.workspace.project

import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.{AutoCompletionResult, ParamValue, PluginContext, PluginParameterAutoCompletionProvider}
import org.silkframework.workspace.WorkspaceReadTrait

/**
  * Auto-completion for workspace projects.
  */
case class ProjectReferenceAutoCompletionProvider() extends PluginParameterAutoCompletionProvider {
  override def autoComplete(searchQuery: String, dependOnParameterValues: Seq[ParamValue],
                            workspace: WorkspaceReadTrait)
                           (implicit context: PluginContext): Traversable[AutoCompletionResult] = {
    implicit val userContext: UserContext = context.user
    filterResults(searchQuery, results(workspace))
  }

  override def valueToLabel(value: String, dependOnParameterValues: Seq[ParamValue],
                            workspace: WorkspaceReadTrait)
                           (implicit context: PluginContext): Option[String] = {
    implicit val userContext: UserContext = context.user
    results(workspace).find(_.value == value).flatMap(_.label)
  }

  private def results(workspace: WorkspaceReadTrait)(implicit userContext: UserContext): Seq[AutoCompletionResult] = {
    workspace.projects.map(_.config) map { projectConfig=>
      val label = projectConfig.metaData.label
      val value = projectConfig.id.toString
      AutoCompletionResult(value, label)
    }
  }
}
