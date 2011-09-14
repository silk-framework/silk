package de.fuberlin.wiwiss.silk.output

import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import de.fuberlin.wiwiss.silk.linkspec.similarity.{Comparison, Aggregation}
import de.fuberlin.wiwiss.silk.linkspec.input.PathInput
import de.fuberlin.wiwiss.silk.instance.Instance

/**
 * Represents a link between two instances.
 *
 * @param source the source URI
 * @param target the target URI
 * @param details
 */
class Link(source: String, target: String,
           val details: Option[Link.Confidence] = None,
           val instances: Option[SourceTargetPair[Instance]] = None) extends SourceTargetPair[String](source, target) {

  def this(source: String, target: String, confidence: Double) = this (source, target, Some(Link.SimpleConfidence(Some(confidence))))

  def this(source: String, target: String, confidence: Double, instances: SourceTargetPair[Instance]) = this (source, target, Some(Link.SimpleConfidence(Some(confidence))), Some(instances))

  /**
   * The confidence that this link is correct. Allowed values: [-1.0, 1.0].
   */
  def confidence: Double = details match {
    case Some(c) => c.value.getOrElse(-1.0)
    case None => -1.0
  }

  override def reverse = new Link(target, source, details)

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
}

object Link {

  sealed trait Confidence {
    val value: Option[Double]
  }

  case class SimpleConfidence(value: Option[Double]) extends Confidence

  case class AggregatorConfidence(value: Option[Double], aggregation: Aggregation, children: Seq[Confidence]) extends Confidence

  case class ComparisonConfidence(value: Option[Double], comparison: Comparison, sourceInput: InputValue, targetInput: InputValue) extends Confidence

  case class InputValue(input: PathInput, values: Traversable[String])

}
