package org.silkframework.runtime.templating

import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.{AutoCompletionResult, PluginParameterAutoCompletionProvider}
import org.silkframework.workspace.WorkspaceReadTrait

class TemplateEngineAutocompletionProvider extends PluginParameterAutoCompletionProvider {

  /** Auto-completion based on a text based search query */
  override def autoComplete(searchQuery: String,
                            projectId: String,
                            dependOnParameterValues: Seq[String],
                            workspace: WorkspaceReadTrait)
                           (implicit userContext: UserContext): Traversable[AutoCompletionResult] = {
    val multiSearchWords = extractSearchTerms(searchQuery)
    TemplateEngines.availableEngines
      .filter(r => matchesSearchTerm(multiSearchWords, r.toLowerCase))
      .map(r => AutoCompletionResult(r, None))
  }

  /** Returns the label if exists for the given auto-completion value. This is needed if a value should
    * be presented to the user and the actual internal value is e.g. not human-readable.
    *
    * @param projectId               The project ID for context.
    * @param value                   The value of the parameter.
    * @param dependOnParameterValues The parameter values this parameter auto-completion depends on.
    * */
  override def valueToLabel(projectId: String,
                            value: String,
                            dependOnParameterValues: Seq[String],
                            workspace: WorkspaceReadTrait)
                           (implicit userContext: UserContext): Option[String] = {
    None
  }
}
