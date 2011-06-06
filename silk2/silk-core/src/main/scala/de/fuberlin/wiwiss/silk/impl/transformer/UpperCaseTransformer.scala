package de.fuberlin.wiwiss.silk.impl.transformer

import de.fuberlin.wiwiss.silk.linkspec.input.SimpleTransformer
import de.fuberlin.wiwiss.silk.util.strategy.StrategyAnnotation

@StrategyAnnotation(id = "upperCase", label = "Upper case", description = "Converts a string to upper case.")
class UpperCaseTransformer() extends SimpleTransformer
{
  override def evaluate(value : String) =
  {
    value.toUpperCase
  }
}
