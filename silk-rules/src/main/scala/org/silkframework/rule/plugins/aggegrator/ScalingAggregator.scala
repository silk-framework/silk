package org.silkframework.rule.plugins.aggegrator

import org.silkframework.entity.Index
import org.silkframework.rule.annotations.{AggregatorExample, AggregatorExamples}
import org.silkframework.rule.similarity.{SimilarityScore, SingleValueAggregator, WeightedSimilarityScore}
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}

@Plugin(
  id = "scale",
  categories = Array(),
  label = "Scale",
  description = "Scales a similarity score by a factor."
)
@AggregatorExamples(Array(
  new AggregatorExample(
    description = "Scales similarity scores by the specified factor.",
    parameters = Array("factor", "0.5"),
    inputs = Array(1.0),
    output = 0.5
  ),
  new AggregatorExample(
    description = "Ignores missing values",
    inputs = Array(Double.NaN),
    output = Double.NaN
  ),
  new AggregatorExample(
    description = "Throws a validation error if more than one input is provided.",
    inputs = Array(0.1, 0.2),
    output = Double.NaN,
    throwsException = classOf[java.lang.IllegalArgumentException]
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
