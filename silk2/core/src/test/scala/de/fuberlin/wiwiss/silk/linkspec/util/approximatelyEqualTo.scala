package de.fuberlin.wiwiss.silk.linkspec.util

import org.scalatest.matchers.{MatchResult, BeMatcher}

/**
 * Matcher to test if 2 values are approximately equal.
 */
case class approximatelyEqualTo(r : Double) extends BeMatcher[Double]
{
    val epsilon = 0.001

    def apply(l: Double) =
        MatchResult(
            compare(l, r),
            l + " is not approximately equal to " + r,
            l + " is approximately equal to " + r
        )

    private def compare(l : Double, r : Double) : Boolean =
    {
        math.abs(l - r) < epsilon
    }
}
