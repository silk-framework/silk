package org.silkframework.rule.plugins.aggegrator

import org.silkframework.entity.Index
import org.silkframework.rule.similarity.{SimilarityScore, SingleValueAggregator, WeightedSimilarityScore}
import org.silkframework.runtime.plugin.annotations.Plugin

@Plugin(
  id = "negate",
  categories = Array("All"),
  label = "Negate",
  description = "Negates the result of the input comparison. A single input is expected. " +
    "Using this operator can have a performance impact, since it lowers the efficiency of the underlying computation."
)
case class NegationAggregator() extends SingleValueAggregator {

  override def evaluateValue(value: WeightedSimilarityScore): SimilarityScore = {
    value.score match {
      case Some(score) =>
        SimilarityScore(0.0 - score)
      case None =>
        SimilarityScore(1.0)
    }

  }

  /* Since it's impossible for the aggregator to know how to create an inverse index, map to default index */
  override def combineIndexes(index1: Index, index2: Index): Index = Index.default

  override def preProcessIndexes(indexes: Seq[Index]): Seq[Index] = Seq(Index.default)
}
