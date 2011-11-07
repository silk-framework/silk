package de.fuberlin.wiwiss.silk.plugins.transformer

import de.fuberlin.wiwiss.silk.util.plugin.Plugin
import de.fuberlin.wiwiss.silk.linkagerule.input.SimpleTransformer

@Plugin(id = "capitalize", label = "Capitalize", description = "Capitalizes the string i.e. converts the first character to upper case.")
class CapitalizeTransformer() extends SimpleTransformer {
  override def evaluate(value: String) = {
    value.capitalize
  }
}