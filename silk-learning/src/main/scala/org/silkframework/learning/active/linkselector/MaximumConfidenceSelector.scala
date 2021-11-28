package org.silkframework.learning.active.linkselector

import org.silkframework.learning.active.LinkCandidate
import org.silkframework.rule.evaluation.ReferenceEntities

import scala.util.{Random, Try}

case class MaximumConfidenceSelector() extends LinkSelector {

  override def apply(rules: Seq[WeightedLinkageRule],
                     unlabeledLinks: Seq[LinkCandidate],
                     referenceEntities: ReferenceEntities)
                    (implicit random: Random): Seq[LinkCandidate] = {
    val sortedCandidates = unlabeledLinks.sortBy(-confidenceWithoutNumbers(_)).take(3)
    sortedCandidates
  }

  /**
    * Summarizes the scores of all matches but ignores pure numbers
    */
  private def confidenceWithoutNumbers(link: LinkCandidate): Double = {
    var sum = 0.0
    for(matchingPair <- link.matchingValues) {
      if(!Try(matchingPair.normalizedSourceValue.toDouble).isSuccess && !Try(matchingPair.normalizedTargetValue.toDouble).isSuccess) {
        sum += matchingPair.score
      }
    }
    sum
  }
}
