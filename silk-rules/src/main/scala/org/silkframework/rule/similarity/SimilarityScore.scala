package org.silkframework.rule.similarity

case class SimilarityScore(score: Option[Double])

object SimilarityScore {

  val none: SimilarityScore = {
    SimilarityScore(None)
  }

  def apply(score: Double): SimilarityScore = {
    SimilarityScore(Some(score))
  }

}

case class WeightedSimilarityScore(score: Option[Double], weight: Int = 1) {

  def unweighted: SimilarityScore = {
    SimilarityScore(score)
  }

}

object WeightedSimilarityScore {

  def apply(score: Double, weight: Int): WeightedSimilarityScore = {
    WeightedSimilarityScore(Some(score), weight)
  }

}
