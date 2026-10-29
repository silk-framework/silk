package org.silkframework.runtime.plugin.types.autoComlpetionProviders

import org.silkframework.runtime.plugin.{AutoCompletionResult, FixedValuesAutoCompletionProvider}

import java.text.DateFormat
import scala.collection.immutable.ArraySeq

/** Auto-completion for supported locales. */
class LocaleParameterAutoCompletionProvider extends FixedValuesAutoCompletionProvider(LocaleParameterAutoCompletionProvider.localeCandidates)

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
}
