package org.silkframework.learning.active.linkselector

import org.silkframework.entity.Link
import org.silkframework.evaluation.ReferenceEntities

/**
 * Selects the links with the best aggregated confidence over all linkage rules.
 */
case class MaximumAgreementSelector() extends LinkSelector {
  def apply(rules: Seq[WeightedLinkageRule], unlabeledLinks: Seq[Link], referenceEntities: ReferenceEntities): Seq[Link] = {
    val rankedLinks = unlabeledLinks.map ( l => (rankLink(rules, l), l))
    // Order descending by aggregated confidence
    val descOrderedLinks = rankedLinks.sortBy(_._1).reverse.map(_._2)

    descOrderedLinks.take(3)
  }

  /**
   * Ranks a link by updating its confidence to the distance from the closes reference link.
   */
  def rankLink(rules: Seq[WeightedLinkageRule], link: Link): Double = {
    // Sum of confidence values of all linkage rules
    rules.map(rule => rule(link.entities.get)).sum
  }
}
