package org.silkframework.learning.active.poolgenerator

import org.silkframework.entity.EntitySchema
import org.silkframework.learning.active.LinkCandidate
import org.silkframework.learning.active.comparisons.ComparisonPair
import org.silkframework.rule.LinkSpec
import org.silkframework.util.DPair

private object LinkPoolGeneratorUtils {

  /**
    * Generates an entity schema from a link spec, which has all provided paths.
    */
  def entitySchema(linkSpec: LinkSpec, paths: Seq[ComparisonPair]): DPair[EntitySchema] = DPair(
    source = linkSpec.entityDescriptions.source.copy(typedPaths = paths.map(_.source).distinct.toIndexedSeq),
    target = linkSpec.entityDescriptions.target.copy(typedPaths = paths.map(_.target).distinct.toIndexedSeq)
  )

  /**
    * Enriches a sequence of links with new links by recombining the linked entities.
    */
  def shuffleLinks(links: Seq[LinkCandidate]): Seq[LinkCandidate] = {
    if(links.nonEmpty) {
      val shuffledLinks = for ((s, t) <- links zip (links.tail :+ links.head)) yield LinkCandidate(s.sourceEntity, t.targetEntity)
      links ++ shuffledLinks
    } else {
      links
    }
  }

}
