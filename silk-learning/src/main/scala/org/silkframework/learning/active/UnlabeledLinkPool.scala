package org.silkframework.learning.active

import org.silkframework.entity.{EntitySchema, Link}
import org.silkframework.util.DPair

/**
 * A pool of unlabeled link candidates.
 *
 * @param entityDescs The schemata of the two data sources.
 * @param links The unlabeled link candidates.
 */
case class UnlabeledLinkPool(entityDescs: DPair[EntitySchema], links: Seq[Link]) {

  def isEmpty: Boolean = links.isEmpty

  def withLinks(addLinks: Seq[Link]): UnlabeledLinkPool = copy(links = (links ++ addLinks).distinct)

  def withoutLinks(removeLinks: Set[Link]): UnlabeledLinkPool = copy(links = links.filterNot(removeLinks.contains))

}

object UnlabeledLinkPool {

  def empty: UnlabeledLinkPool = UnlabeledLinkPool(DPair.fill(EntitySchema.empty), Seq.empty)

}
