package org.silkframework.rule.plugins.aggegrator

import org.silkframework.entity.Index
import org.silkframework.rule.similarity.Aggregator
import org.silkframework.runtime.plugin.annotations.Plugin

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

  /* Since it's impossible for the aggregator to know how to create an inverse index, map to default index */
  override def combineIndexes(index1: Index, index2: Index): Index = Index.default

  override def preProcessIndexes(indexes: Seq[Index]): Seq[Index] = Seq(Index.default)
}
