package de.fuberlin.wiwiss.silk.linkspec.aggregator

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import de.fuberlin.wiwiss.silk.linkspec.aggegrator.MaximumAggregator
import de.fuberlin.wiwiss.silk.linkspec.util.approximatelyEqualTo

class MaximumAggregatorTest extends FlatSpec with ShouldMatchers
{
    val aggregator = new MaximumAggregator()

    "MaximumAggregator" should "return the maximum" in
    {
        aggregator.evaluate((1, 1.0) :: (1, 1.0) :: (1, 1.0) :: Nil).get should be (approximatelyEqualTo (1.0))
        aggregator.evaluate((1, 1.0) :: (1, 0.0) :: Nil).get should be (approximatelyEqualTo (1.0))
        aggregator.evaluate((1, 0.4) :: (1, 0.5) :: (1, 0.6) :: Nil).get should be (approximatelyEqualTo (0.6))
        aggregator.evaluate((1, 0.0) :: (1, 0.0) :: Nil).get should be (approximatelyEqualTo (0.0))
    }
}
