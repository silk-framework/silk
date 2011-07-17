package de.fuberlin.wiwiss.silk.impl.transformer

import de.fuberlin.wiwiss.silk.util.strategy.StrategyAnnotation
import de.fuberlin.wiwiss.silk.linkspec.input.Transformer

@StrategyAnnotation(id = "removeValues", label = "Remove values", description = "Removes values.")
class FilterValues(blacklist : String) extends Transformer
{
  val filterValues = blacklist.split(",").map(_.trim.toLowerCase).toSet

  override def apply(values : Seq[Traversable[String]]) =
  {
    values.head.map(_.toLowerCase).filterNot(filterValues.contains)
  }
}