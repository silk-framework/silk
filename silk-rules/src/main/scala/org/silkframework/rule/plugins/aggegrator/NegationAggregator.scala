package org.silkframework.rule.plugins.aggegrator

import org.silkframework.entity.Index
import org.silkframework.rule.similarity.Aggregator
import org.silkframework.runtime.plugin.Plugin

@Plugin(
  id = "negate",
  categories = Array("All"),
  label = "Negate",
  description = "Negates the result of the first input comparison. All other inputs are ignored."
)
case class NegationAggregator() extends Aggregator {

  override def evaluate(values: Traversable[(Int, Double)]): Option[Double] = {
    if (values.isEmpty) {
      None
    } else {
      require(values.size == 1, "Accepts exactly one input")
      Some(0.0 - values.head._2)
    }
  }

  override def combineIndexes(index1: Index, index2: Index): Index = index1 disjunction index2
}
