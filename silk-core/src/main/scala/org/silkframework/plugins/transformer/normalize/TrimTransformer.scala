package org.silkframework.plugins.transformer.normalize

import org.silkframework.rule.input.SimpleTransformer
import org.silkframework.runtime.plugin.Plugin

@Plugin(
  id = "trim",
  categories = Array("Normalize","Substring"),
  label = "Trim",
  description = "Remove leading and trailing whitespaces."
)
case class TrimTransformer() extends SimpleTransformer {
  override def evaluate(value: String) = value.trim
}
