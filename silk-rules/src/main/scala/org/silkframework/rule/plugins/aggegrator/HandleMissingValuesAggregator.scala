package org.silkframework.rule.plugins.aggegrator

import org.silkframework.entity.Index
import org.silkframework.rule.similarity.{Aggregator, SimilarityScore, SingleValueAggregator, WeightedSimilarityScore}
import org.silkframework.runtime.plugin.annotations.Plugin

@Plugin(
  id = "handleMissingValues",
  categories = Array("All"),
  label = "Handle missing values",
  description = "TODO."
)
case class HandleMissingValuesAggregator(defaultValue: Boolean) extends SingleValueAggregator {

  private val defaultSimilarity = if(defaultValue) 1.0 else -1.0

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