package org.silkframework.learning.active.linkselector

import org.silkframework.learning.active.{LinkCandidate, MatchingValues}
import org.silkframework.rule.evaluation.ReferenceEntities

import scala.util.{Random, Try}

/**
  * Selects the link candidate with the most matches.
  */
case class BestMatchSelector() extends LinkSelector {

  override def apply(rules: Seq[WeightedLinkageRule],
                     unlabeledLinks: Seq[LinkCandidate],
                     referenceEntities: ReferenceEntities)
                    (implicit random: Random): Seq[LinkCandidate] = {
    val sortedCandidates = unlabeledLinks.sortBy(linkCandidateScore).take(3)
    sortedCandidates
  }

  private def linkCandidateScore(linkCandidate: LinkCandidate): Double = {
    var score = 1.0
    for(matchingValues <- linkCandidate.matchingValues) {
      if(isNumberMatch(matchingValues)) {
        // We weight number matches lower
        score += 0.5
      } else {
        score += 1.0
      }
    }
    1.0 / score
  }

  @inline
  private def isNumberMatch(matchingPair: MatchingValues): Boolean = {
    Try(matchingPair.normalizedSourceValue.toDouble).isSuccess && Try(matchingPair.normalizedTargetValue.toDouble).isSuccess
  }
}
