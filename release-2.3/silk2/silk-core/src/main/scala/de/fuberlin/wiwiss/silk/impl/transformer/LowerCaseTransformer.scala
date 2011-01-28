package de.fuberlin.wiwiss.silk.impl.transformer

import de.fuberlin.wiwiss.silk.linkspec.input.Transformer
import de.fuberlin.wiwiss.silk.util.strategy.StrategyAnnotation

@StrategyAnnotation(id = "lowerCase", label = "Lower case", description = "Converts a string to lower case.")
class LowerCaseTransformer() extends Transformer
{
  override def evaluate(strings : Seq[String]) =
  {
    strings.head.toLowerCase
  }
}
