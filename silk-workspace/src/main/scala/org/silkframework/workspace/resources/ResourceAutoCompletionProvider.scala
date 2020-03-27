package org.silkframework.workspace.resources

import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.AutoCompletionResult
import org.silkframework.workspace.WorkspacePluginParameterAutoCompletionProvider

/**
  * Auto-completion for project resources.
  */
case class ResourceAutoCompletionProvider() extends WorkspacePluginParameterAutoCompletionProvider {
  override protected def autoComplete(searchQuery: String, projectId: String, dependOnParameterValues: Seq[String])
                                     (implicit userContext: UserContext): Traversable[AutoCompletionResult] = {
    val multiSearchWords = extractSearchTerms(searchQuery)
    project(projectId).resources.list
        .filter(r => matchesSearchTerm(multiSearchWords, r.toLowerCase))
        .map(r => AutoCompletionResult(r, None))
  }

  override def valueToLabel(projectId: String, value: String, dependOnParameterValues: Seq[String])
                           (implicit userContext: UserContext): Option[String] = {
    None
  }
}
