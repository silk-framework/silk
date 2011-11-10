package de.fuberlin.wiwiss.silk.entity

import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.linkagerule.similarity.{Comparison, Aggregation}
import de.fuberlin.wiwiss.silk.linkagerule.input.PathInput

/**
 * Represents a link between two entities.
 *
 * @param source The source URI
 * @param target The target URI
 * @param confidence (Optional) The confidence that this link is correct. Allowed values: [-1.0, 1.0].
 * @param entities (Optional) The entities which are interlinked.
 */
class Link(source: String,
           target: String,
           val confidence: Option[Double] = None,
           val entities: Option[DPair[Entity]] = None) extends DPair[String](source, target) {

  /**
   * Reverses the source and the target of this link.
   */
  override def reverse = new Link(target, source, confidence, entities)

  override def toString = "<" + source + ">  <" + target + ">"

  /**
   * Compares two Links for equality.
   * Two Links are considered equal if their source and target URIs match.
   */
  override def equals(other: Any) = other match {
    case otherLink: Link => otherLink.source == source && otherLink.target == target
    case _ => false
  }

  override def hashCode = (source + target).hashCode

  def update(source: String = source,
            target: String = target,
            confidence: Option[Double] = confidence,
            entities: Option[DPair[Entity]] = entities) = new Link(source, target, confidence, entities)
}
