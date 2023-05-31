package org.silkframework.rule.plugins.aggegrator

import org.silkframework.entity.Index
import org.silkframework.rule.annotations.{AggregatorExample, AggregatorExamples}
import org.silkframework.rule.similarity.{SimilarityScore, SingleValueAggregator, WeightedSimilarityScore}
import org.silkframework.runtime.plugin.PluginCategories
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}

@Plugin(
  id = "handleMissingValues",
  categories = Array(PluginCategories.recommended),
  label = "Handle missing values",
  description = "Generates a default similarity score, if no similarity score is provided (e.g., due to missing values). " +
    "Using this operator can have a performance impact, since it lowers the efficiency of the underlying computation."
)
@AggregatorExamples(Array(
  new AggregatorExample(
    description = "Forwards input similarity scores.",
    inputs = Array(0.1),
    output = 0.1
  ),
  new AggregatorExample(
    description = "Outputs the default score, if no input score is provided.",
    parameters = Array("defaultValue", "1.0"),
    inputs = Array(Double.NaN),
    output = 1.0
  )
))
case class HandleMissingValuesAggregator(
  @Param("The default value to be generated, if no similarity score is provided. Must be a value between -1 (inclusive) and 1 (inclusive). '1' represents boolean true and '-1' represents boolean false.")
  defaultValue: Double = -1.0) extends SingleValueAggregator {
  require(defaultValue >= -1.0 && defaultValue <= 1.0, "Default value must be between -1 (inclusive) and 1 (inclusive).")

  private val defaultSimilarity = defaultValue

  override def evaluateValue(value: WeightedSimilarityScore): SimilarityScore = {
    if (value.score.isEmpty) {
      SimilarityScore(defaultSimilarity)
    } else {
      value.unweighted
    }
  }

  override def combineIndexes(index1: Index, index2: Index): Index = Index.default

  override def preProcessIndexes(indexes: Seq[Index]): Seq[Index] = Seq(Index.default)
}