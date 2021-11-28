package org.silkframework.learning.active.linkselector

import org.silkframework.learning.active.LinkCandidate
import org.silkframework.rule.evaluation.ReferenceEntities

import scala.util.Random

/**
 * Created by andreas on 2/8/16.
 */
case class LinkSelectorCombinator(pickLinkSelector: (Seq[WeightedLinkageRule], Seq[LinkCandidate], ReferenceEntities) => LinkSelector) extends LinkSelector {
  def apply(rules: Seq[WeightedLinkageRule], unlabeledLinks: Seq[LinkCandidate], referenceEntities: ReferenceEntities)(implicit random: Random): Seq[LinkCandidate] = {
    val linkSelector = pickLinkSelector(rules, unlabeledLinks, referenceEntities)
    linkSelector(rules, unlabeledLinks, referenceEntities)
  }
}
