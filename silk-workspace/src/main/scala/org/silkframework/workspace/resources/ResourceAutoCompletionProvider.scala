package org.silkframework.workspace.resources

import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.{AutoCompletionResult, ParamValue, PluginContext, PluginParameterAutoCompletionProvider}
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.workspace.WorkspaceReadTrait

/**
  * Auto-completion for project resources.
  */
case class ResourceAutoCompletionProvider() extends PluginParameterAutoCompletionProvider {
  override def autoComplete(searchQuery: String, dependOnParameterValues: Seq[ParamValue],
                            workspace: WorkspaceReadTrait)
                           (implicit context: PluginContext): Iterable[AutoCompletionResult] = {
    val multiSearchWords = extractSearchTerms(searchQuery)
    implicit val userContext: UserContext = context.user
    val projectId = context.projectId.getOrElse(throw new ValidationException("Project not provided"))
    workspace.project(projectId).resources.list
        .filter(r => matchesSearchTerm(multiSearchWords, r.toLowerCase))
        .map(r => AutoCompletionResult(r, None))
  }

  override def valueToLabel(value: String, dependOnParameterValues: Seq[ParamValue],
                            workspace: WorkspaceReadTrait)
                           (implicit context: PluginContext): Option[String] = {
    None
  }
}
