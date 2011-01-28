package de.fuberlin.wiwiss.silk.impl.transformer

import de.fuberlin.wiwiss.silk.linkspec.input.Transformer
import de.fuberlin.wiwiss.silk.util.strategy.StrategyAnnotation

@StrategyAnnotation(id = "upperCase", label = "Upper case", description = "Converts a string to upper case.")
class UpperCaseTransformer() extends Transformer
{
  override def evaluate(strings : Seq[String]) =
  {
    strings.head.toUpperCase
  }
}
