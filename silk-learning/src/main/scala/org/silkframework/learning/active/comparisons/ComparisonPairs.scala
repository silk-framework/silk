package org.silkframework.learning.active.comparisons

import org.silkframework.entity.paths.TypedPath
import org.silkframework.util.DPair

import scala.language.implicitConversions

/**
  * Holds the current comparison pairs.
  *
  * @param suggestedPairs The comparison pairs that have been suggested by the algorithm.
  * @param selectedPairs The comparison paris that have been selected by the user.
  */
case class ComparisonPairs(suggestedPairs: Seq[ComparisonPair],
                           selectedPairs: Seq[ComparisonPair],
                           randomSeed: Long = 0L)


trait ComparisonPair {

  def source: TypedPath

  def target: TypedPath

  def plain: PlainComparisonPair = {
    PlainComparisonPair(source, target)
  }

}

object ComparisonPair {

  /**
    * Converts this comparison pair to a pair of typed paths.
    */
  implicit def toPair(pair: ComparisonPair): DPair[TypedPath] = {
    DPair(pair.source, pair.target)
  }


  def apply(source: TypedPath, target: TypedPath): ComparisonPair = {
    PlainComparisonPair(source, target)
  }

  def unapply(pair: ComparisonPair): Option[(TypedPath, TypedPath)] = {
    Some(pair.source, pair.target)
  }

}

case class PlainComparisonPair(source: TypedPath, target: TypedPath) extends ComparisonPair

case class ComparisonPairWithExamples(source: TypedPath,
                                      target: TypedPath,
                                      score: Double,
                                      sourceExamples: Set[String] = Set.empty,
                                      targetExamples: Set[String] = Set.empty) extends ComparisonPair {

  def merge(other: ComparisonPairWithExamples, maximumExamples: Int = 3): ComparisonPairWithExamples = {
    ComparisonPairWithExamples(
      source = source,
      target = target,
      score = score + other.score,
      sourceExamples = if(sourceExamples.size < maximumExamples) sourceExamples ++ other.sourceExamples else sourceExamples,
      targetExamples = if(targetExamples.size < maximumExamples) targetExamples ++ other.targetExamples else targetExamples,
    )
  }

}

object ComparisonPairs {

  /**
    * Initial empty comparison pairs.
    */
  def initial(randomSeed: Long): ComparisonPairs = {
    ComparisonPairs(Seq.empty, Seq.empty, randomSeed = randomSeed)
  }
}


