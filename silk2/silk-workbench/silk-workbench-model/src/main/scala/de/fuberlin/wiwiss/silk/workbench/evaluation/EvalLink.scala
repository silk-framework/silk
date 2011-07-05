package de.fuberlin.wiwiss.silk.workbench.evaluation

import de.fuberlin.wiwiss.silk.output.Link
import de.fuberlin.wiwiss.silk.workbench.evaluation.EvalLink.{LinkType, Correctness}

/**
 * An evaluation link.
 */
class EvalLink(link : Link, val correct : Correctness, val linkType : LinkType) extends Link(link.sourceUri, link.targetUri, link.confidence, link.details)
{
}

object EvalLink
{
  /**
   * The correctness of a link
   */
  sealed trait Correctness

  /**
   * Correct link.
   */
  case object Correct extends Correctness

  /**
   * Incorrect link.
   */
  case object Incorrect extends Correctness

  /**
   * Correctness unknown.
   */
  case object Unknown extends Correctness

  /**
   * The type of link.
   */
  sealed trait LinkType

  /**
   * Link which have been generate by Silk.
   */
  case object Generated extends LinkType

  /**
   * ReferenceType link.
   */
  trait ReferenceType extends LinkType

  /**
   * Positive reference link.
   */
  case object Positive extends ReferenceType

  /**
   * Negative reference link.
   */
  case object Negative extends ReferenceType
}