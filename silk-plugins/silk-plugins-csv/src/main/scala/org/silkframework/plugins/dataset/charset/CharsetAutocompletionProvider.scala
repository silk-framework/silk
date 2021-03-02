package org.silkframework.plugins.dataset.charset

import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.{AutoCompletionResult, PluginParameterAutoCompletionProvider}
import org.silkframework.workspace.WorkspaceReadTrait

/**
  * Autocompletion provider that completes available charsets.
  * Only suggest primary names and ignores aliases (including aliases currently spams the UI with too many similar names).
  */
case class CharsetAutocompletionProvider() extends PluginParameterAutoCompletionProvider {

  override def autoComplete(searchQuery: String, projectId: String, dependOnParameterValues: Seq[String],
                            workspace: WorkspaceReadTrait)
                           (implicit userContext: UserContext): Traversable[AutoCompletionResult] = {
    val multiSearchWords = extractSearchTerms(searchQuery)
    CharsetUtils.charsetNames
      .filter(r => matchesSearchTerm(multiSearchWords, r.toLowerCase))
      .map(r => AutoCompletionResult(r, None))
  }

  override def valueToLabel(projectId: String, value: String, dependOnParameterValues: Seq[String],
                            workspace: WorkspaceReadTrait)
                           (implicit userContext: UserContext): Option[String] = {
    None
  }
}
