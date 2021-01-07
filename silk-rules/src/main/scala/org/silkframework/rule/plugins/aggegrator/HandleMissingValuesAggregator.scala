package org.silkframework.rule.plugins.aggegrator

import org.silkframework.entity.Index
import org.silkframework.rule.similarity.Aggregator
import org.silkframework.runtime.plugin.annotations.Plugin

@Plugin(
  id = "handleMissingValues",
  categories = Array("All"),
  label = "handle missing values",
  description = "TODO."
)
case class HandleMissingValuesAggregator(defaultValue: Double) extends Aggregator {

  override def evaluate(values: Traversable[(Int, Double)]): Option[Double] = {
    if (values.isEmpty) {
      Some(defaultValue)
    } else {
      require(values.size == 1, "Accepts exactly one input")
      val value = values.head._2
      if(value == Double.NegativeInfinity) {
        Some(defaultValue)
      } else {
        Some(values.head._2)
      }
    }
  }

  override def combineIndexes(index1: Index, index2: Index): Index = Index.default

  override def preProcessIndexes(indexes: Seq[Index]): Seq[Index] = Seq(Index.default)
}