package de.fuberlin.wiwiss.silk.output

import de.fuberlin.wiwiss.silk.instance.Path

/**
 * Represents a link between two instances.
 *
 * @param sourceUri the source URI
 * @param targetUri the target URI
 * @param details
 */
class Link(val sourceUri : String, val targetUri : String, val details : Option[Link.Confidence] = None)
{
  def this(sourceUri : String, targetUri : String, confidence : Double) = this(sourceUri, targetUri, Some(Link.SimpleConfidence(Some(confidence))))

  /**
   * The confidence that this link is correct. Allowed values: [-1.0, 1.0].
   */
  def confidence : Double = details match
  {
    case Some(c) => c.value.getOrElse(-1.0)
    case None => -1.0
  }

  override def toString = "<" + sourceUri + ">  <" + targetUri + ">"

  /**
   * Compares two Links for equality.
   * Two Links are considered equal if their source and target URIs match.
   */
  override def equals(other : Any) = other match
  {
    case otherLink : Link => otherLink.sourceUri == sourceUri && otherLink.targetUri == targetUri
    case _ => false
  }

  override def hashCode = (sourceUri + targetUri).hashCode
}

object Link
{
  sealed trait Confidence
  {
    val value : Option[Double]
  }

  case class SimpleConfidence(value : Option[Double]) extends Confidence

  case class AggregatorConfidence(value : Option[Double], function : String, children : Seq[Confidence]) extends Confidence

  case class ComparisonConfidence(value : Option[Double], function : String, sourceInput : InputValue, targetInput : InputValue) extends Confidence

  case class InputValue(path : Path, values : Traversable[String])
}
