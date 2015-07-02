package de.fuberlin.wiwiss.silk.plugins.transformer.normalize

import de.fuberlin.wiwiss.silk.linkagerule.input.SimpleTransformer
import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin

@Plugin(
  id = "trim",
  categories = Array("Normalize","Substring"),
  label = "Trim",
  description = "Remove leading and trailing whitespaces."
)
case class TrimTransformer() extends SimpleTransformer {
  override def evaluate(value: String) = value.trim
}
