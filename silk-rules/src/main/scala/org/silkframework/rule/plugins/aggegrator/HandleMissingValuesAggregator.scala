package org.silkframework.rule.plugins.aggegrator

import org.silkframework.entity.Index
import org.silkframework.rule.similarity.Aggregator
import org.silkframework.runtime.plugin.annotations.Plugin

@Plugin(
  id = "handleMissingValues",
  categories = Array("All"),
  label = "Handle missing values",
  description = "TODO."
)
case class HandleMissingValuesAggregator(defaultValue: Boolean) extends Aggregator {

  private val defaultSimilarity = if(defaultValue) 1.0 else -1.0

  override def evaluate(values: Traversable[(Int, Double)]): Option[Double] = {
    if (values.isEmpty) {
      Some(defaultSimilarity)
    } else {
      require(values.size == 1, "Accepts exactly one input")
      val value = values.head._2
      if(value == Double.NegativeInfinity) {
        Some(defaultSimilarity)
      } else {
        Some(values.head._2)
      }
    }
  }

  override def combineIndexes(index1: Index, index2: Index): Index = Index.default

  override def preProcessIndexes(indexes: Seq[Index]): Seq[Index] = Seq(Index.default)
}