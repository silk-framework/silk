package de.fuberlin.wiwiss.silk.impl.transformer

import de.fuberlin.wiwiss.silk.util.strategy.StrategyAnnotation
import de.fuberlin.wiwiss.silk.util.StringUtils._
import de.fuberlin.wiwiss.silk.linkspec.input.SimpleTransformer

@StrategyAnnotation(id = "logarithm", label = "Logarithm", description = "Transforms all numbers by applying the logarithm function. Non-numeric values are left unchanged.")
class LogarithmTransformer(base : Int = 10) extends SimpleTransformer
{
  override def evaluate(value : String) =
  {
    value match
    {
      case DoubleLiteral(d) => (math.log(d) / math.log(base)).toString
      case str => str
    }
  }
}