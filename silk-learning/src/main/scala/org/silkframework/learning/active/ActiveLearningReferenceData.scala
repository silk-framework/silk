package org.silkframework.learning.active

import org.silkframework.entity.{EntitySchema, Link, LinkDecision, LinkWithEntities, ReferenceLink}
import org.silkframework.rule.evaluation.ReferenceEntities
import org.silkframework.util.DPair

/**
  * Contains the current training data for the active learning.
  * This includes the labeled and unlabeled link candidates.
  */
case class ActiveLearningReferenceData(entitySchemata: DPair[EntitySchema],
                                       linkCandidates: Seq[LinkCandidate],
                                       referenceLinks: Seq[ReferenceLink] = Seq.empty,
                                       randomSeed: Long) {

  def positiveLinks: Seq[ReferenceLink] = {
    referenceLinks.filter(_.decision == LinkDecision.POSITIVE)
  }

  def negativeLinks: Seq[ReferenceLink] = {
    referenceLinks.filter(_.decision == LinkDecision.NEGATIVE)
  }

  def withPositiveLink(link: LinkWithEntities): ActiveLearningReferenceData = {
    copy(linkCandidates = linkCandidates.filterNot(_ == link),
         referenceLinks = ReferenceLink(link, LinkDecision.POSITIVE) +: referenceLinks.filterNot(_ == link))
  }

  def withNegativeLink(link: LinkWithEntities): ActiveLearningReferenceData = {
    copy(linkCandidates = linkCandidates.filterNot(_ == link),
         referenceLinks = ReferenceLink(link, LinkDecision.NEGATIVE) +: referenceLinks.filterNot(_ == link))
  }

  def withoutLink(link: Link): ActiveLearningReferenceData = {
    copy(referenceLinks = referenceLinks.filterNot(_ == link))
  }

  def findLink(sourceUri: String, targetUri: String): Option[LinkWithEntities] = {
    val allLinks = referenceLinks ++ linkCandidates
    allLinks.find(l => l.source == sourceUri && l.target == targetUri)
  }

  def toReferenceEntities: ReferenceEntities = {
    val positiveEntities = positiveLinks.map(_.linkEntities)
    val negativeEntities = negativeLinks.map(_.linkEntities)
    ReferenceEntities.fromEntities(positiveEntities, negativeEntities)
  }

}

object ActiveLearningReferenceData {

  def empty: ActiveLearningReferenceData = ActiveLearningReferenceData(DPair.fill(EntitySchema.empty), Seq.empty, Seq.empty, 0L)

}
