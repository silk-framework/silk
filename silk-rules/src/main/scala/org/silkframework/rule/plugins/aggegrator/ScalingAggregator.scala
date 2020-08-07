package org.silkframework.rule.plugins.aggegrator

import org.silkframework.entity.Index
import org.silkframework.rule.similarity.Aggregator
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}

@Plugin(
  id = "scale",
  categories = Array("All"),
  label = "Scale",
  description = "Scales the result of the first input. All other inputs are ignored."
)
case class ScalingAggregator(
  @Param("All input similarity values are multiplied with this factor.")
  factor: Double) extends Aggregator {

  override def evaluate(values: Traversable[(Int, Double)]): Option[Double] = {
    if (values.isEmpty) {
      None
    } else {
      require(values.size == 1, "Accepts exactly one input")
      Some(values.head._2 * factor)
    }
  }

  override def combineIndexes(index1: Index, index2: Index): Index = index1

  override def preProcessIndexes(indexes: Seq[Index]): Seq[Index] = indexes
}
