package de.fuberlin.wiwiss.silk.impl.transformer

import de.fuberlin.wiwiss.silk.linkspec.input.SimpleTransformer
import de.fuberlin.wiwiss.silk.util.strategy.StrategyAnnotation

@StrategyAnnotation(id = "stripPrefix", label = "Strip prefix", description = "Strips a prefix of a string.")
class StripPrefixTransformer(prefix : String) extends SimpleTransformer
{
  override def evaluate(value : String) : String =
  {
    value.stripPrefix(prefix)
  }
}