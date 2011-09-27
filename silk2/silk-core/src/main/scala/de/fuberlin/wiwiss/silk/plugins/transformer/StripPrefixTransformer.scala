package de.fuberlin.wiwiss.silk.plugins.transformer

import de.fuberlin.wiwiss.silk.linkagerule.input.SimpleTransformer
import de.fuberlin.wiwiss.silk.util.plugin.Plugin

@Plugin(id = "stripPrefix", label = "Strip prefix", description = "Strips a prefix of a string.")
class StripPrefixTransformer(prefix: String) extends SimpleTransformer {
  override def evaluate(value: String): String = {
    value.stripPrefix(prefix)
  }
}