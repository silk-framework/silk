package org.silkframework.workspace.resources

import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.{AutoCompletionResult, PluginParameterAutoCompletionProvider}
import org.silkframework.workspace.WorkspaceReadTrait

/**
  * Auto-completion for project resources.
  */
case class ResourceAutoCompletionProvider() extends PluginParameterAutoCompletionProvider {
  override protected def autoComplete(searchQuery: String, projectId: String, dependOnParameterValues: Seq[String],
                                      workspace: WorkspaceReadTrait)
                                     (implicit userContext: UserContext): Traversable[AutoCompletionResult] = {
    val multiSearchWords = extractSearchTerms(searchQuery)
    workspace.project(projectId).resources.list
        .filter(r => matchesSearchTerm(multiSearchWords, r.toLowerCase))
        .map(r => AutoCompletionResult(r, None))
  }

  override def valueToLabel(projectId: String, value: String, dependOnParameterValues: Seq[String],
                            workspace: WorkspaceReadTrait)
                           (implicit userContext: UserContext): Option[String] = {
    None
  }
}
