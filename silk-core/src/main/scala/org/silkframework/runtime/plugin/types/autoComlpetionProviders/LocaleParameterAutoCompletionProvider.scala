package org.silkframework.runtime.plugin.types.autoComlpetionProviders

import org.silkframework.runtime.plugin.{AutoCompletionResult, ParamValue, PluginContext, PluginParameterAutoCompletionProvider}
import org.silkframework.workspace.WorkspaceReadTrait

import java.text.DateFormat
import scala.collection.immutable.ArraySeq

/** Auto-completion for supported locales. */
class LocaleParameterAutoCompletionProvider extends PluginParameterAutoCompletionProvider {


  /** Auto-completion based on a text based search query */
  override def autoComplete(searchQuery: String, dependOnParameterValues: Seq[ParamValue], workspace: WorkspaceReadTrait)
                           (implicit context: PluginContext): Iterable[AutoCompletionResult] = {
    filterResults(searchQuery, LocaleParameterAutoCompletionProvider.localeCandidates)
  }

  /** Returns the label if exists for the given auto-completion value. This is needed if a value should
    * be presented to the user and the actual internal value is e.g. not human-readable.
    *
    * @param value                   The value of the parameter.
    * @param dependOnParameterValues The parameter values this parameter auto-completion depends on.
    * */
  override def valueToLabel(value: String, dependOnParameterValues: Seq[ParamValue], workspace: WorkspaceReadTrait)
                           (implicit context: PluginContext): Option[String] = {
    LocaleParameterAutoCompletionProvider.localeCandidateLabels.get(value)
  }
}

object LocaleParameterAutoCompletionProvider {
  private lazy val localeCandidates: Seq[AutoCompletionResult] = {
    val availableLocales = ArraySeq.unsafeWrapArray(DateFormat.getAvailableLocales)
    availableLocales.map { locale =>
      val value = locale.toString
      AutoCompletionResult(
        value,
        Some(s"$value (${locale.getDisplayName})")
      )
    }.sortBy(_.value).drop(1) // Drop empty locale, we already have the empty string for None
  }

  private lazy val localeCandidateLabels: Map[String, String] = {
    localeCandidates.map(l => (l.value, l.label.getOrElse(""))).toMap
  }
}