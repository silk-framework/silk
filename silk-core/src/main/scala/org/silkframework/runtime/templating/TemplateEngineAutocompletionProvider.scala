package org.silkframework.runtime.templating

import org.silkframework.runtime.plugin.{AutoCompletionResult, ParamValue, PluginContext, PluginParameterAutoCompletionProvider, PluginRegistry}
import org.silkframework.workspace.WorkspaceReadTrait

class TemplateEngineAutocompletionProvider extends PluginParameterAutoCompletionProvider {

  /** Auto-completion based on a text based search query */
  override def autoComplete(searchQuery: String,
                            dependOnParameterValues: Seq[ParamValue],
                            workspace: WorkspaceReadTrait)
                           (implicit context: PluginContext): Iterable[AutoCompletionResult] = {
    val multiSearchWords = extractSearchTerms(searchQuery)
    TemplateEngines.availableEngines
      .filter(_ != DisabledTemplateEngine.id) // Disabled template engine should not be suggested to the user
      .filter(_ != UnresolvedTemplateEngine.id) // Unresolved template engine should not be suggested to the user
      .filter(r => matchesSearchTerm(multiSearchWords, r.toLowerCase))
      .map(r => AutoCompletionResult(r, None))
  }

  /** Returns the label if exists for the given auto-completion value. This is needed if a value should
    * be presented to the user and the actual internal value is e.g. not human-readable.
    *
    * @param value                   The value of the parameter.
    * @param dependOnParameterValues The parameter values this parameter auto-completion depends on.
    * */
  override def valueToLabel(value: String,
                            dependOnParameterValues: Seq[ParamValue],
                            workspace: WorkspaceReadTrait)
                           (implicit context: PluginContext): Option[String] = {
    PluginRegistry.pluginByIdOpt[TemplateEngine](value).map(_.label)
  }
}
