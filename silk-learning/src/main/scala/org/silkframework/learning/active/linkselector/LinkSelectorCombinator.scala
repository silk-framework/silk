package org.silkframework.learning.active.linkselector

import org.silkframework.learning.active.{ActiveLearningReferenceData, LinkCandidate}

import scala.util.Random

/**
 * Created by andreas on 2/8/16.
 */
case class LinkSelectorCombinator(pickLinkSelector: (Seq[WeightedLinkageRule], ActiveLearningReferenceData) => LinkSelector) extends LinkSelector {
  def apply(rules: Seq[WeightedLinkageRule], referenceData: ActiveLearningReferenceData)(implicit random: Random): Seq[LinkCandidate] = {
    val linkSelector = pickLinkSelector(rules, referenceData)
    linkSelector(rules, referenceData)
  }
}
