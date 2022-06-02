package org.silkframework.rule.similarity

import org.silkframework.entity.Entity
import org.silkframework.util.DPair

import scala.collection.mutable.ArrayBuffer

trait SimpleAggregator extends Aggregator {

  def apply(operators: Seq[SimilarityOperator], entities: DPair[Entity], limit: Double): SimilarityScore = {
    val weightedValues = new ArrayBuffer[WeightedSimilarityScore](operators.size)
    for(op <- operators) {
      val score = op(entities, limit)
      weightedValues += WeightedSimilarityScore(score, op.weight)
    }

    evaluate(weightedValues)
  }

  def evaluate(values: Seq[WeightedSimilarityScore]): SimilarityScore

}
