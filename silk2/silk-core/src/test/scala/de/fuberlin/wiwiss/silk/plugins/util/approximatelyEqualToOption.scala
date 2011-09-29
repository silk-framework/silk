package de.fuberlin.wiwiss.silk.plugins.util

import org.scalatest.matchers.{MatchResult, BeMatcher}

/**
 * Matcher to test if 2 values are approximately equal.
 */
case class approximatelyEqualToOption(r: Option[Double]) extends BeMatcher[Option[Double]] {
  val epsilon = 0.001

  def apply(l: Option[Double]) = (r,l) match {
    case (Some(x), Some(y)) => approximatelyEqualTo(x)(y)
    case _ => {
      MatchResult(
        l.isDefined == r.isDefined,
        l + " is not approximately equal to " + r,
        l + " is approximately equal to " + r
      )
    }
  }
}