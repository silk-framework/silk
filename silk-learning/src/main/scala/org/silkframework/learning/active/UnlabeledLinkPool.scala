package org.silkframework.learning.active

import org.silkframework.entity.{EntitySchema, Link}
import org.silkframework.entity.rdf.SparqlEntitySchema
import org.silkframework.util.DPair

/**
 * A pool of unlabeled link candidates.
 *
 * @param entityDescs The schemata of the two data sources.
 * @param links The unlabeled link candidates.
 */
case class UnlabeledLinkPool(entityDescs: DPair[EntitySchema], links: Traversable[Link]) {

  def isEmpty = links.isEmpty

  def withoutLinks(removeLinks: Set[Link]) = copy(links = links.filterNot(removeLinks.contains))

}

object UnlabeledLinkPool {

  def empty = UnlabeledLinkPool(DPair.fill(EntitySchema.empty), Traversable.empty)

}
