package org.silkframework.learning.active.poolgenerator

import org.silkframework.entity.{EntitySchema, Link, LinkWithEntities}
import org.silkframework.entity.paths.TypedPath
import org.silkframework.rule.LinkSpec
import org.silkframework.util.DPair

private object LinkPoolGeneratorUtils {

  /**
    * Generates an entity schema from a link spec, which has all provided paths.
    */
  def entitySchema(linkSpec: LinkSpec, paths: Seq[DPair[TypedPath]]): DPair[EntitySchema] = DPair(
    source = linkSpec.entityDescriptions.source.copy(typedPaths = paths.map(_.source).distinct.toIndexedSeq),
    target = linkSpec.entityDescriptions.target.copy(typedPaths = paths.map(_.target).distinct.toIndexedSeq)
  )

  /**
    * Enriches a sequence of links with new links by recombining the linked entities.
    */
  def shuffleLinks(links: Seq[Link]): Seq[Link] = {
    if(links.nonEmpty) {
      val shuffledLinks = for ((s, t) <- links zip (links.tail :+ links.head)) yield new LinkWithEntities(s.source, t.target, DPair(s.entities.get.source, t.entities.get.target))
      links ++ shuffledLinks
    } else {
      links
    }
  }

}
