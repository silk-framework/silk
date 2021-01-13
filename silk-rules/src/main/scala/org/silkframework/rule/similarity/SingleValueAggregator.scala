package org.silkframework.rule.similarity

trait SingleValueAggregator extends Aggregator {

  def evaluateValue(value: WeightedSimilarityScore): SimilarityScore

  override def evaluate(values: Seq[WeightedSimilarityScore]): SimilarityScore = {
    if (values.isEmpty) {
      SimilarityScore.none
    } else {
      require(values.size == 1, "Accepts exactly one input")
      evaluateValue(values.head)
    }
  }
}
