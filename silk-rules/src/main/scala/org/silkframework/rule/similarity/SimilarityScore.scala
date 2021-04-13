package org.silkframework.rule.similarity

/**
  * A similarity score.
  *
  * @param score A value between -1 (inclusive) and 1 (inclusive). Higher values imply a better match:
  *              0 is the minimum score to consider two values a match.
  *              1 denotes a perfect match.
  *              May be [[None]], if no similarity score could be computed (e.g., because of missing values)
  */
case class SimilarityScore(score: Option[Double])

object SimilarityScore {

  val none: SimilarityScore = {
    SimilarityScore(None)
  }

  def apply(score: Double): SimilarityScore = {
    SimilarityScore(Some(score))
  }

}

/**
  * A [[SimilarityScore]] that is annotated with a weight.
  */
case class WeightedSimilarityScore(score: Option[Double], weight: Int = 1) {

  /**
    * Converts this weighted score to a plain similarity score.
    */
  def unweighted: SimilarityScore = {
    SimilarityScore(score)
  }

}

object WeightedSimilarityScore {

  def apply(score: Double, weight: Int): WeightedSimilarityScore = {
    WeightedSimilarityScore(Some(score), weight)
  }

}
