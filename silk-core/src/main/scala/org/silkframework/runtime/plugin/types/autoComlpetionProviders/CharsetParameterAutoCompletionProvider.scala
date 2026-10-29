package org.silkframework.runtime.plugin.types.autoComlpetionProviders

import org.silkframework.runtime.plugin.FixedValuesAutoCompletionProvider

import java.nio.charset.Charset
import scala.jdk.CollectionConverters.CollectionHasAsScala

/**
  * Provides autocomplete suggestions for charset parameters.
  * Suggests the canonical names of all charsets supported by the JVM; aliases remain valid values.
  */
class CharsetParameterAutoCompletionProvider extends FixedValuesAutoCompletionProvider(CharsetParameterAutoCompletionProvider.charsetNames)

object CharsetParameterAutoCompletionProvider {
  private lazy val charsetNames: Seq[String] = Charset.availableCharsets().keySet().asScala.toSeq
}
