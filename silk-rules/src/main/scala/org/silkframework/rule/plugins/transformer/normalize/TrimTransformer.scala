package org.silkframework.rule.plugins.transformer.normalize

import org.silkframework.rule.input.SimpleTransformer
import org.silkframework.runtime.plugin.annotations.{Plugin, PluginReference}

@Plugin(
  id = TrimTransformer.pluginId,
  categories = Array("Normalize","Substring"),
  label = "Trim",
  description = "Remove leading and trailing whitespaces.",
  relatedPlugins = Array(
    new PluginReference(
      id = RemoveBlanksTransformer.pluginId,
      description = "Trim removes all whitespace characters from the edges of a string: spaces, tabs, and newlines at the start or end are cleared, but the interior is left untouched. Remove blanks removes only plain space characters, but does so throughout the entire string, including the middle."
    )
  )
)
case class TrimTransformer() extends SimpleTransformer {
  override def evaluate(value: String): String = value.trim
}

object TrimTransformer {
  final val pluginId = "trim"
}
