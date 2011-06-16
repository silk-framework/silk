package de.fuberlin.wiwiss.silk.output

import de.fuberlin.wiwiss.silk.instance.Path

/**
 * Represents a link between two instances.
 *
 * @param sourceUri the source URI
 * @param targetUri the target URI
 * @param confidence the confidence that this link is correct. Allowed values: [0.0, 1.0]
 */
class Link(val sourceUri : String, val targetUri : String, val confidence : Double = 0.0, val details : Option[Link.Similarity] = None)
{
  require(confidence >= -1.0 && confidence <= 1.0, "confidence >= -1.0 && confidence <= 1.0 (confidence=" + confidence)

  override def toString = "<" + sourceUri + ">  <" + targetUri + "> (" + confidence + ")"

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
  sealed trait Similarity
  {
    val function : String
    val value : Option[Double]
  }

  case class AggregatorSimilarity(function : String, value : Option[Double], children : Seq[Similarity]) extends Similarity

  case class ComparisonSimilarity(function : String, value : Option[Double], sourceInput : InputValue, targetInput : InputValue) extends Similarity

  case class InputValue(path : Path, values : Traversable[String])
}
