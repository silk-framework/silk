package org.silkframework.learning.active.poolgenerator

import org.silkframework.entity.{EntitySchema, Link}
import org.silkframework.learning.active.LinkCandidate
import org.silkframework.util.DPair

/**
 * A pool of unlabeled link candidates.
 *
 * @param entityDescs The schemata of the two data sources.
 * @param links The unlabeled link candidates.
 */
case class UnlabeledLinkPool(entityDescs: DPair[EntitySchema], linkCandidates: Seq[LinkCandidate]) {

  def isEmpty: Boolean = linkCandidates.isEmpty

  def withoutLinks(removeLinks: Set[Link]): UnlabeledLinkPool = copy(linkCandidates = linkCandidates.filterNot(removeLinks.contains))

}

object UnlabeledLinkPool {

  def empty: UnlabeledLinkPool = UnlabeledLinkPool(DPair.fill(EntitySchema.empty), Seq.empty)

}
