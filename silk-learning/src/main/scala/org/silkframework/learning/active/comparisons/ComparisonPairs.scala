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
                           randomSeed: Long = 0)

/**
  * A single comparison pair.
  *
  * @param source The path from the first dataset.
  * @param target The path from the second dataset.
  */
case class ComparisonPair(source: TypedPath, target: TypedPath)

object ComparisonPair {

  /**
    * Converts this comparison pair to a pair of typed paths.
    */
  implicit def toPair(pair: ComparisonPair): DPair[TypedPath] = {
    DPair(pair.source, pair.target)
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

