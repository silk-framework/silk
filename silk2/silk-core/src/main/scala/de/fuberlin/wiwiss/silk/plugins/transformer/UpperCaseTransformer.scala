package de.fuberlin.wiwiss.silk.plugins.transformer

import de.fuberlin.wiwiss.silk.linkspec.input.SimpleTransformer
import de.fuberlin.wiwiss.silk.util.plugin.Plugin

@Plugin(id = "upperCase", label = "Upper case", description = "Converts a string to upper case.")
class UpperCaseTransformer() extends SimpleTransformer {
  override def evaluate(value: String) = {
    value.toUpperCase
  }
}
