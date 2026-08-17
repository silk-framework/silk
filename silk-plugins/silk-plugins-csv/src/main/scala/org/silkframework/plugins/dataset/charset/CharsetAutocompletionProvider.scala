package org.silkframework.plugins.dataset.charset

import org.silkframework.runtime.plugin.FixedValuesAutoCompletionProvider

/**
  * Autocompletion provider that completes available charsets, including the DI-specific UTF-8-BOM.
  * Only suggests primary names and ignores aliases (including aliases currently spams the UI with too many similar names).
  */
case class CharsetAutocompletionProvider() extends FixedValuesAutoCompletionProvider(CharsetUtils.charsetNames)
