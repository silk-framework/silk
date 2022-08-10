package org.silkframework.learning.active

import org.silkframework.entity.{EntitySchema, Link}
import org.silkframework.rule.evaluation.ReferenceEntities
import org.silkframework.util.DPair

/**
  * Contains the current training data for the active learning.
  * This includes the labeled and unlabeled link candidates.
  */
case class ActiveLearningReferenceData(entitySchemata: DPair[EntitySchema],
                                       linkCandidates: Seq[LinkCandidate],
                                       positiveLinks: Seq[Link] = Seq.empty,
                                       negativeLinks: Seq[Link] = Seq.empty,
                                       randomSeed: Long) {

  def withPositiveLink(link: Link): ActiveLearningReferenceData = {
    require(link.entities.isDefined, "Cannot add link that does not have entities attached.")
    copy(positiveLinks = (link +: positiveLinks).distinct,
         negativeLinks = negativeLinks.filterNot(_ == link))
  }

  def withNegativeLink(link: Link): ActiveLearningReferenceData = {
    require(link.entities.isDefined, "Cannot add link that does not have entities attached.")
    copy(positiveLinks = positiveLinks.filterNot(_ == link),
         negativeLinks = (link +: negativeLinks).distinct)
  }

  def withoutLink(link: Link): ActiveLearningReferenceData = {
    copy(positiveLinks = positiveLinks.filterNot(_ == link),
         negativeLinks = negativeLinks.filterNot(_ == link))
  }

  def findLink(sourceUri: String, targetUri: String): Option[Link] = {
    val allLinks = positiveLinks ++ negativeLinks ++ linkCandidates
    allLinks.find(l => l.source == sourceUri && l.target == targetUri)
  }

  def toReferenceEntities: ReferenceEntities = {
    val positiveEntities = positiveLinks.map(_.entities.get)
    val negativeEntities = negativeLinks.map(_.entities.get)
    ReferenceEntities.fromEntities(positiveEntities, negativeEntities)
  }

}

object ActiveLearningReferenceData {

  def empty: ActiveLearningReferenceData = ActiveLearningReferenceData(DPair.fill(EntitySchema.empty), Seq.empty, Seq.empty, Seq.empty, 0L)

}
