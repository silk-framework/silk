package org.silkframework.learning.active

import org.silkframework.entity.paths.TypedPath
import org.silkframework.entity.{Entity, Link, LinkDecision, ReferenceLink}
import org.silkframework.util.DPair

/**
  * A link candidate that has been generated because the values of a pair of values matched.
  */
case class LinkCandidate(sourceEntity: Entity, targetEntity: Entity,
                         matchingValues: Seq[MatchingValues] = Seq.empty,
                         override val confidence: Option[Double] = None) extends ReferenceLink(sourceEntity.uri, targetEntity.uri, DPair(sourceEntity, targetEntity), LinkDecision.UNLABELED) {

  /** The optional pair of entities (values) */
  override def entities: Option[DPair[Entity]] = Some(DPair(sourceEntity, targetEntity))

  /** Flip the source and target entity */
  override def reverse: Link = LinkCandidate(targetEntity, sourceEntity, matchingValues.map(_.reverse), confidence)

  def withMatch(pair: MatchingValues): LinkCandidate = {
    if(matchingValues.exists(m => m.sourcePathIndex == pair.sourcePathIndex && m.targetPathIndex == pair.targetPathIndex)) {
      this
    } else {
      copy(matchingValues = matchingValues :+ pair)
    }
  }

  def withConfidence(confidence: Double): LinkCandidate = {
    copy(confidence = Some(confidence))
  }
}

object LinkCandidate {

  def fromLink(link: Link): LinkCandidate = {
    link.entities match {
      case Some(entities) =>
        LinkCandidate(entities.source, entities.target)
      case None =>
        throw new IllegalArgumentException("Cannot generate link candidates from links that haven't the original entities attached.")
    }
  }

}

case class MatchingValues(sourcePathIndex: Int, targetPathIndex: Int,
                          normalizedSourceValue: String, normalizedTargetValue: String,
                          score: Double = 0.0) {

  def sourcePath(sourceEntity: Entity): TypedPath = sourceEntity.schema.typedPaths(sourcePathIndex)

  def targetPath(targetEntity: Entity): TypedPath = targetEntity.schema.typedPaths(targetPathIndex)

  def reverse: MatchingValues = MatchingValues(targetPathIndex, sourcePathIndex, normalizedTargetValue, normalizedSourceValue)

}