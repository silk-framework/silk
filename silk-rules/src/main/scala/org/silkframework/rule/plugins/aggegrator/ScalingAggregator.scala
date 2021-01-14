package org.silkframework.rule.plugins.aggegrator

import org.silkframework.entity.Index
import org.silkframework.rule.similarity.{SimilarityScore, SingleValueAggregator, WeightedSimilarityScore}
import org.silkframework.runtime.plugin.annotations.{AggregatorExample, AggregatorExamples, Param, Plugin}

@Plugin(
  id = "scale",
  categories = Array("All"),
  label = "Scale",
  description = "Scales the result of the first input. All other inputs are ignored."
)
@AggregatorExamples(Array(
  new AggregatorExample(
    parameters = Array("factor", "0.5"),
    inputs = Array(1.0),
    output = 0.5
  ),
  new AggregatorExample(
    parameters = Array("factor", "0.1"),
    inputs = Array(0.1),
    output = 0.01
  ),
  new AggregatorExample(
    inputs = Array(Double.NaN),
    output = Double.NaN
  )
))
case class ScalingAggregator(
  @Param("All input similarity values are multiplied with this factor.")
  factor: Double = 1.0) extends SingleValueAggregator {

  require(factor >= 0.0 && factor <= 1.0, "Scaling factor must be a value between 0.0 and 1.0.")

  override def evaluateValue(value: WeightedSimilarityScore): SimilarityScore = {
    SimilarityScore(value.score.map(score => factor * score))
  }

  override def combineIndexes(index1: Index, index2: Index): Index = index1

  override def preProcessIndexes(indexes: Seq[Index]): Seq[Index] = indexes
}
