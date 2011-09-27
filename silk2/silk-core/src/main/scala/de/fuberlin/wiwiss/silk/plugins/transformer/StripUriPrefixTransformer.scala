package de.fuberlin.wiwiss.silk.plugins.transformer

import de.fuberlin.wiwiss.silk.linkspec.input.SimpleTransformer
import de.fuberlin.wiwiss.silk.util.plugin.Plugin
import math.max

@Plugin(id = "stripUriPrefix", label = "Strip URI prefix", description = "Strips the URI prefix of a string.")
class StripUriPrefixTransformer() extends SimpleTransformer {
  override def evaluate(value: String): String = {
    val uriPrefixEnd = max(value.lastIndexOf("/"), value.lastIndexOf("#"))
    if (uriPrefixEnd > -1)
      value.substring(uriPrefixEnd + 1, value.size)
    else
      value
  }
}
