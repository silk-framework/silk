package de.fuberlin.wiwiss.silk.plugins.transformer

import de.fuberlin.wiwiss.silk.linkagerule.input.SimpleTransformer
import de.fuberlin.wiwiss.silk.util.plugin.Plugin

@Plugin(id = "replace", label = "Replace", description = "Replace all occurrences of a string \"search\" with \"replace\" in a string.")
class ReplaceTransformer(search: String, replace: String) extends SimpleTransformer {
  override def evaluate(value: String) = {
    value.replace(search, replace)
  }
}
