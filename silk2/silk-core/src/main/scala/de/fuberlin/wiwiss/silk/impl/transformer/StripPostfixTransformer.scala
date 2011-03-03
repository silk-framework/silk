package de.fuberlin.wiwiss.silk.impl.transformer

import de.fuberlin.wiwiss.silk.linkspec.input.Transformer
import de.fuberlin.wiwiss.silk.util.strategy.StrategyAnnotation

@StrategyAnnotation(id = "stripPostfix", label = "Strip postfix", description = "Strips a postfix of a string.")
class StripPostfixTransformer(postfix : String) extends Transformer
{
  override def evaluate(strings : Seq[String]) : String =
  {
    strings.head.stripSuffix(postfix)
  }
}