package de.fuberlin.wiwiss.silk.impl.transformer

import de.fuberlin.wiwiss.silk.linkspec.input.SimpleTransformer
import de.fuberlin.wiwiss.silk.util.strategy.StrategyAnnotation
import math.max

@StrategyAnnotation(id = "stripUriPrefix", label = "Strip URI prefix", description = "Strips the URI prefix of a string.")
class StripUriPrefixTransformer() extends SimpleTransformer {
  override def evaluate(value: String): String = {
    val uriPrefixEnd = max(value.lastIndexOf("/"), value.lastIndexOf("#"))
    if (uriPrefixEnd > -1)
      value.substring(uriPrefixEnd + 1, value.size)
    else
      value
  }
}
