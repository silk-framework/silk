package de.fuberlin.wiwiss.silk.impl.transformer

import de.fuberlin.wiwiss.silk.linkspec.input.SimpleTransformer
import de.fuberlin.wiwiss.silk.util.plugin.Plugin

@Plugin(id = "lowerCase", label = "Lower case", description = "Converts a string to lower case.")
class LowerCaseTransformer() extends SimpleTransformer {
  override def evaluate(value: String) = {
    value.toLowerCase
  }
}
