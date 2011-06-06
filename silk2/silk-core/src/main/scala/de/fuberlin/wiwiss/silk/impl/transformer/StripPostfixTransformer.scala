package de.fuberlin.wiwiss.silk.impl.transformer

import de.fuberlin.wiwiss.silk.linkspec.input.SimpleTransformer
import de.fuberlin.wiwiss.silk.util.strategy.StrategyAnnotation

@StrategyAnnotation(id = "stripPostfix", label = "Strip postfix", description = "Strips a postfix of a string.")
class StripPostfixTransformer(postfix : String) extends SimpleTransformer
{
  override def evaluate(value : String) : String =
  {
    value.stripSuffix(postfix)
  }
}