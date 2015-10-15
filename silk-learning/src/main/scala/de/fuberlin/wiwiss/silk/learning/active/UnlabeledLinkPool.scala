package de.fuberlin.wiwiss.silk.learning.active

import de.fuberlin.wiwiss.silk.entity.{EntityDescription, Link}
import de.fuberlin.wiwiss.silk.util.DPair

/**
 * A pool of unlabeled link candidates.
 *
 * @param entityDescs The schemata of the two data sources.
 * @param links The unlabeled link candidates.
 */
case class UnlabeledLinkPool(entityDescs: DPair[EntityDescription], links: Traversable[Link]) {

  def isEmpty = links.isEmpty

  def withoutLinks(removeLinks: Set[Link]) = copy(links = links.filterNot(removeLinks.contains))

}

object UnlabeledLinkPool {

  def empty = UnlabeledLinkPool(DPair.empty, Traversable.empty)

}
